package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.Template
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLClassLoader
import kotlin.jvm.Transient

@CacheableTask
abstract class GenerateAssertions : DefaultTask() {
    /**
     * Classes and packages to generate assertions for.
     */
    @get:Input
    abstract val classOrPackageNames: ListProperty<String>

    /**
     * The sourceSet containing classes to generate assertions for.
     * Stored as a plain field: SourceSet is not configuration-cache serializable, so it
     * is only read during configuration (transient to keep the cache entry clean).
     */
    @Transient
    private var sourceSetField: SourceSet? = null

    var sourceSet: SourceSet?
        @Internal
        get() = sourceSetField ?: extension().sourceSet.getOrNull()
        set(value) {
            sourceSetField = value
        }

    /**
     * The target sourceSet for generated assertions.
     * Stored as a plain field: SourceSet is not configuration-cache serializable, so it
     * is only read during configuration (transient to keep the cache entry clean).
     */
    @Transient
    private var testSourceSetField: SourceSet? = null

    var testSourceSet: SourceSet?
        @Internal
        get() = testSourceSetField ?: extension().testSourceSet.getOrNull()
        set(value) {
            testSourceSetField = value
        }

    /**
     * Destination package for entry point classes. The generator will choose if unset.
     */
    @get:Input
    @get:Optional
    abstract val entryPointPackage: Property<String>

    /**
     * Output directory for generated classes. Defaults to
     * [buildDirectory]/generated/sources/assertj/[testSourceSet.name].
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * What kinds of entry point classes to generate.
     */
    @get:Input
    abstract val entryPointTypes: SetProperty<AssertionsEntryPointType>

    /**
     * Entry point classes inherit from core Assertj classes
     */
    @get:Input
    abstract val entryPointInherits: Property<Boolean>

    /**
     * Clean output directory before generating assertions.
     */
    @get:Input
    abstract val cleanOutputDir: Property<Boolean>

    /**
     * Generate assertions for non-public fields and properties as well.
     */
    @get:Input
    abstract val generateForNonPublicFields: Property<Boolean>

    /**
     * Regexes for classes to be excluded
     */
    @get:Input
    abstract val excludes: ListProperty<String>

    /**
     * The runtime classpath of [sourceSet], used to load the classes to generate
     * assertions for. Populated by the plugin during wiring.
     */
    @get:InputFiles
    @get:CompileClasspath
    abstract val classPath: ConfigurableFileCollection

    init {
        group = "assertj"
        description = "Generate Assertj Assertions"
    }

    private fun extension(): AssertjGeneratorExtension = project.extensions.getByType(AssertjGeneratorExtension::class.java)

    private fun getTemplate(entryPointType: AssertionsEntryPointType): Template {
        val templateType =
            when (entryPointType) {
                AssertionsEntryPointType.STANDARD -> Template.Type.ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.BDD -> Template.Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.SOFT -> Template.Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.JUNIT_SOFT -> Template.Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.BDD_SOFT -> Template.Type.BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.JUNIT_BDD_SOFT -> Template.Type.JUNIT_BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT -> Template.Type.AUTO_CLOSEABLE_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
                AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT -> Template.Type.AUTO_CLOSEABLE_BDD_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
            }

        val fileName = "${entryPointType.name.lowercase()}_assertions_entry_point_class.txt"
        val templateContent =
            this.javaClass.classLoader
                .getResource(fileName)
                ?.readText()
                ?: throw RuntimeException("Error locating resource $fileName!")
        return Template(templateType, templateContent)
    }

    @TaskAction
    fun generateAssertions() {
        val outDir = outputDir.get().asFile
        if (cleanOutputDir.get()) {
            outDir.deleteRecursively()
        }
        val descriptionConverter = ClassToClassDescriptionConverter()
        val assertionGenerator = BaseAssertionGenerator()
        if (generateForNonPublicFields.get()) {
            assertionGenerator.setGenerateAssertionsForAllFields(true)
        }
        assertionGenerator.setDirectoryWhereAssertionFilesAreGenerated(outDir)
        if (entryPointInherits.get()) {
            entryPointTypes.get().forEach {
                assertionGenerator.register(getTemplate(it))
            }
        }
        val classLoader = URLClassLoader(classPath.map { it.toURI().toURL() }.toTypedArray())
        /*
         * e.g. Kotlin enums link against kotlin.enums.EnumEntries from the classpath.
         * Class collection and description conversion must run inside the loader's
         * lifetime — after use {} the loader is closed and later reflection over the
         * loaded classes would fail with NoClassDefFoundError. Generation itself only
         * reads the converted descriptions and is safe to run after the loader closes.
         */
        val classDescriptions =
            classLoader.use {
                ClassUtil
                    .collectClasses(classLoader, *classOrPackageNames.get().toTypedArray())
                    .filterNot { classDescription ->
                        excludes.get().any { exclude ->
                            exclude.toRegex().containsMatchIn(classDescription.rawType.name)
                        }
                    }.filterNot { it.rawType.isSynthetic }
                    .map { descriptionConverter.convertToClassDescription(it) }
                    .toSet()
            }

        /*
         * The generator emits @javax.annotation.Generated, which no JDK ships since 11
         * and no modern stack carries; rewrite to the Jakarta namespace unconditionally.
         */
        val generatedAssertions = classDescriptions.map { assertionGenerator.generateCustomAssertionFor(it) }.toSet()
        generatedAssertions.forEach {
            val fixedSource = it.readText().replace("@javax.annotation.Generated", "@jakarta.annotation.Generated")
            it.writeText(fixedSource)
        }
        val entryPoints =
            if (generatedAssertions.isEmpty()) {
                emptySet<File>()
            } else {
                entryPointTypes
                    .get()
                    .map {
                        assertionGenerator.generateAssertionsEntryPointClassFor(
                            classDescriptions,
                            it,
                            entryPointPackage.orNull,
                        )
                    }.toSet()
            }

        logger.debug(
            "Generated ${generatedAssertions.size} assertion classes, " +
                "${entryPoints.size} entry point classes in $outDir",
        )
    }
}
