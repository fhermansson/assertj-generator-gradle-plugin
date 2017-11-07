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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.net.URLClassLoader

open class GenerateAssertions : DefaultTask(), ProjectEvaluationListener {

    private val extension: AssertjGeneratorExtension
        @Internal
        get() = project.extensions.getByType(AssertjGeneratorExtension::class.java)

    /**
     * What kinds of entry point classes to generate.
     */
    var entryPointTypes: Array<AssertionsEntryPointType>? = null
        @Internal
        get() = if (field != null) field else extension.entryPointTypes

    val entryPointTypesAsSet
        @Input
        get() = entryPointTypes!!.toSet()
    /**
     * Output directory for generated classes.
     * Any type accepted by Project.file(Object).
     */
    var outputDir: Any? = null
        @Internal
        get() = if (field != null) field else extension.outputDir

    val resolvedOutputDir: File
        @OutputDirectory
        get() = project.file(outputDir!!)

    /**
     * The name of the sourceSet containing classes to generate assertions for.
     */
    var sourceSetName: String? = null
        @Input
        get() = if (field != null) field else extension.sourceSetName

    /**
     * The name of the target sourceSet for generated assertions.
     */
    var testSourceSetName: String? = null
        @Input
        get() = if (field != null) field else extension.testSourceSetName

    /**
     * Destination package for entry point classes. The generator will choose if null.
     */
    var entryPointPackage: String? = null
        @Input
        @Optional
        get() = if (field != null) field else extension.entryPointPackage

    /**
     * Entry point classes inherit from core Assertj classes
     */
    var entryPointInherits: Boolean? = null
        @Input
        get() = if (field != null) field else extension.entryPointInherits

    /**
     * Classes and packages to generate assertions for.
     */
    var classOrPackageNames: Array<String>? = null
        @Input
        get() = if (field != null) field else extension.classOrPackageNames

    /**
     * Clean output directory before generating assertions.
     */
    var cleanOutputDir: Boolean? = null
        @Input
        get() = if (field != null) field else extension.cleanOutputDir

    init {
        group = "assertj"
        description = "Generate Assertj Assertions"
        project.gradle.addProjectEvaluationListener(this)
    }

    @InputFiles
    @CompileClasspath
    fun getClassPath(): FileCollection {
        return sourceSetByName(sourceSetName!!).runtimeClasspath
    }

    private fun sourceSetByName(name: String): SourceSet {
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        return javaPluginConvention.sourceSets.getByName(name)
    }

    private fun getTemplate(entryPointType: AssertionsEntryPointType): Template {
        val templateType = when (entryPointType) {
            AssertionsEntryPointType.STANDARD -> Template.Type.ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.BDD -> Template.Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.SOFT -> Template.Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            AssertionsEntryPointType.JUNIT_SOFT -> Template.Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
        }

        val fileName = "${entryPointType.name.toLowerCase()}_assertions_entry_point_class.txt"
        val templateContent = this.javaClass.classLoader.getResource(fileName).readText()
        return Template(templateType, templateContent)
    }

    override fun afterEvaluate(project: Project?, state: ProjectState?) {
        val sourceClassesTaskName = sourceSetByName(sourceSetName!!).classesTaskName
        dependsOn.add(sourceClassesTaskName)
        val testSourceSet = sourceSetByName(testSourceSetName!!)
        testSourceSet.java.srcDir(resolvedOutputDir)
        this.project.getTasksByName(testSourceSet.compileJavaTaskName, false).forEach {
            it.dependsOn(this)
        }
        val idea = this.project.extensions.findByType(IdeaModel::class.java)
        if (idea !== null) {
            idea.module.generatedSourceDirs.add(resolvedOutputDir)
        }
    }

    override fun beforeEvaluate(project: Project?) {
    }


    @TaskAction
    fun generateAssertions() {
        if (cleanOutputDir!!) {
            project.delete(resolvedOutputDir)
        }
        val descriptionConverter = ClassToClassDescriptionConverter()
        val assertionGenerator = BaseAssertionGenerator()
        assertionGenerator.setDirectoryWhereAssertionFilesAreGenerated(resolvedOutputDir.absolutePath)
        if (entryPointInherits!!) {
            entryPointTypesAsSet.forEach {
                assertionGenerator.register(getTemplate(it))
            }
        }
        val classLoader = URLClassLoader(getClassPath().map { it.toURI().toURL() }.toTypedArray())
        val classes = ClassUtil.collectClasses(classLoader, *classOrPackageNames!!)
        val classDescriptions = classes.map { descriptionConverter.convertToClassDescription(it) }.toSet()
        val generatedAssertions = classDescriptions.map { assertionGenerator.generateCustomAssertionFor(it) }.toSet()

        val entryPoints =
                if (generatedAssertions.isEmpty())
                    emptySet<File>()
                else
                    entryPointTypesAsSet.map {
                        assertionGenerator.generateAssertionsEntryPointClassFor(classDescriptions, it, entryPointPackage)
                    }.toSet()

        logger.lifecycle("Generated ${generatedAssertions.size} assertion classes, " +
                "${entryPoints.size} entry point classes in ${resolvedOutputDir}")

    }

}