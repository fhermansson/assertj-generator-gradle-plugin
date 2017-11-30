package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

open class AssertjGeneratorExtension(project: Project) {
    /**
     * Classes and packages to generate assertions for.
     */
    var classOrPackageNames: Array<String> = emptyArray()

    /**
     * The sourceSet containing classes to generate assertions for.
     */
    var sourceSet: SourceSet = project.convention.getPlugin(JavaPluginConvention::class.java)
            .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    /**
     * The target sourceSet for generated assertions.
     */
    var testSourceSet: SourceSet = project.convention.getPlugin(JavaPluginConvention::class.java)
            .sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

    /**
     * Destination package for entry point classes. The generator will choose if null.
     */
    var entryPointPackage: String? = null

    /**
     * Output directory for generated classes.
     * Any type accepted by Project.file(Object).
     */
    var outputDir: Any? = null

    /**
     * What kinds of entry point classes to generate.
     */
    var entryPointTypes: Array<AssertionsEntryPointType> = arrayOf(AssertionsEntryPointType.STANDARD)

    /**
     * Entry point classes inherit from core Assertj classes
     */
    var entryPointInherits = true

    /**
     * Clean output directory before generating assertions.
     */
    val cleanOutputDir: Boolean = true
}