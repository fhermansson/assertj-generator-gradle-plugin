package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.tasks.SourceSet

open class AssertjGeneratorExtension {
    /**
     * Classes and packages to generate assertions for.
     */
    var classOrPackageNames: Array<String> = emptyArray()
    /**
     * The name of the sourceSet containing classes to generate assertions for.
     */
    var sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME
    /**
     * The name of the target sourceSet for generated assertions.
     */
    var testSourceSetName = SourceSet.TEST_SOURCE_SET_NAME
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
    @Deprecated(message = "Replace with entryPointTypes = ['STANDARD']")
    var standard = true
        set(value) {
            field = value
            includeEntryPointType(AssertionsEntryPointType.STANDARD, value)
        }
    @Deprecated(message = "Replace with entryPointTypes = ['SOFT']")
    var soft = false
        set(value) {
            field = value
            includeEntryPointType(AssertionsEntryPointType.SOFT, value)
        }
    @Deprecated(message = "Replace with entryPointTypes = ['BDD']")
    var bdd = false
        set(value) {
            field = value
            includeEntryPointType(AssertionsEntryPointType.BDD, value)
        }
    @Deprecated(message = "Replace with entryPointTypes = ['JUNIT_SOFT']")
    var junit_soft = false
        set(value) {
            field = value
            includeEntryPointType(AssertionsEntryPointType.JUNIT_SOFT, value)
        }
    /**
     * Clean output directory before generating assertions.
     */
    val cleanOutputDir: Boolean = true

    private fun includeEntryPointType(type: AssertionsEntryPointType, include: Boolean) {
        val set = entryPointTypes.toMutableSet()
        if (include) {
            set.add(type)
        } else {
            set.remove(type)
        }
        entryPointTypes = set.toTypedArray()
    }
}