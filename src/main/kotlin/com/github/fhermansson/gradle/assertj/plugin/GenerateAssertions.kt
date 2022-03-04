package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.Template
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.net.URLClassLoader

open class GenerateAssertions : DefaultTask(), ProjectEvaluationListener {

    private val extension: AssertjGeneratorExtension by lazy {
        project.extensions.getByType(AssertjGeneratorExtension::class.java)
    }

    val classPath: FileCollection
        @InputFiles
        @CompileClasspath
        get() = sourceSet!!.runtimeClasspath
    /**
     * What kinds of entry point classes to generate.
     */
    var entryPointTypes: Array<AssertionsEntryPointType>? = null
        @Internal
        get() = field ?: extension.entryPointTypes

    val entryPointTypesAsSet
        @Input
        get() = entryPointTypes!!.toSet()
    /**
     * Output directory for generated classes.
     * Any type accepted by Project.file(Object).
     */
    var outputDir: Any? = null
        @Internal
        get() = field ?: extension.outputDir ?: "src/${testSourceSet!!.name}/generated-java"

    val resolvedOutputDir: File
        @OutputDirectory
        get() = project.file(outputDir!!)

    /**
     * The sourceSet containing classes to generate assertions for.
     */
    var sourceSet: SourceSet? = null
        @Internal
        get() = field ?: extension.sourceSet

    /**
     * The target sourceSet for generated assertions.
     */
    var testSourceSet: SourceSet? = null
        @Internal
        get() = field ?: extension.testSourceSet

    /**
     * Destination package for entry point classes. The generator will choose if null.
     */
    var entryPointPackage: String? = null
        @Input
        @Optional
        get() = field ?: extension.entryPointPackage

    /**
     * Entry point classes inherit from core Assertj classes
     */
    var entryPointInherits: Boolean? = null
        @Input
        get() = field ?: extension.entryPointInherits

    /**
     * Classes and packages to generate assertions for.
     */
    var classOrPackageNames: Array<String>? = null
        @Input
        get() = field ?: extension.classOrPackageNames

    /**
     * Clean output directory before generating assertions.
     */
    var cleanOutputDir: Boolean? = null
        @Input
        get() = field ?: extension.cleanOutputDir

    init {
        group = "assertj"
        description = "Generate Assertj Assertions"
        project.gradle.addProjectEvaluationListener(this)
    }

    private fun getTemplate(entryPointType: AssertionsEntryPointType): Template {
        val templateType = when (entryPointType) {
            AssertionsEntryPointType.STANDARD -> Template.Type.ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.BDD -> Template.Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.SOFT -> Template.Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.JUNIT_SOFT -> Template.Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.BDD_SOFT -> Template.Type.BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.JUNIT_BDD_SOFT -> Template.Type.JUNIT_BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT -> Template.Type.AUTO_CLOSEABLE_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT -> Template.Type.AUTO_CLOSEABLE_BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
        }

        val fileName = "${entryPointType.name.toLowerCase()}_assertions_entry_point_class.txt"
        val templateContent = this.javaClass.classLoader.getResource(fileName)?.readText()
            ?: throw RuntimeException("Error locating resource $fileName!")
        return Template(templateType, templateContent)
    }

    override fun afterEvaluate(project: Project, state: ProjectState) {
        if (project == this.project) {
            val sourceClassesTaskName = sourceSet!!.classesTaskName
            dependsOn.add(sourceClassesTaskName)
            testSourceSet!!.java.srcDir(resolvedOutputDir)
            listOf("java", "kotlin", "groovy").forEach { lang ->
                project.getTasksByName(testSourceSet!!.getCompileTaskName(lang), false).forEach {
                    it.dependsOn(this)
                }
            }
            project.extensions.findByType(IdeaModel::class.java)
                ?.module?.generatedSourceDirs?.add(resolvedOutputDir)
        }
    }

    override fun beforeEvaluate(project: Project) {
    }

    @TaskAction
    fun generateAssertions() {
        if (cleanOutputDir!!) {
            project.delete(resolvedOutputDir)
        }
        val descriptionConverter = ClassToClassDescriptionConverter()
        val assertionGenerator = BaseAssertionGenerator()
        assertionGenerator.setDirectoryWhereAssertionFilesAreGenerated(resolvedOutputDir)
        if (entryPointInherits!!) {
            entryPointTypesAsSet.forEach {
                assertionGenerator.register(getTemplate(it))
            }
        }
        val classLoader = URLClassLoader(classPath.map { it.toURI().toURL() }.toTypedArray())
        val classes = ClassUtil.collectClasses(classLoader, *classOrPackageNames!!)
            .filterNot { it.rawType.isSynthetic }

        val classDescriptions = classes.map { descriptionConverter.convertToClassDescription(it) }.toSet()
        val generatedAssertions = classDescriptions.map { assertionGenerator.generateCustomAssertionFor(it) }.toSet()

        val entryPoints =
            if (generatedAssertions.isEmpty())
                emptySet<File>()
            else
                entryPointTypesAsSet.map {
                    assertionGenerator.generateAssertionsEntryPointClassFor(classDescriptions, it, entryPointPackage)
                }.toSet()

        logger.lifecycle(
            "Generated ${generatedAssertions.size} assertion classes, " +
                "${entryPoints.size} entry point classes in $resolvedOutputDir"
        )
    }
}
