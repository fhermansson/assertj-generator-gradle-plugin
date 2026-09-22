package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.SourceSet

abstract class AssertjGeneratorExtension {
    /**
     * Classes and packages to generate assertions for.
     */
    abstract val classOrPackageNames: ListProperty<String>

    /**
     * The sourceSet containing classes to generate assertions for.
     */
    abstract val sourceSet: Property<SourceSet>

    /**
     * The target sourceSet for generated assertions.
     */
    abstract val testSourceSet: Property<SourceSet>

    /**
     * Destination package for entry point classes. The generator will choose if unset.
     */
    abstract val entryPointPackage: Property<String>

    /**
     * Output directory for generated classes. Defaults to
     * [buildDirectory]/generated/sources/assertj/[testSourceSet.name].
     */
    abstract val outputDir: DirectoryProperty

    /**
     * What kinds of entry point classes to generate.
     */
    abstract val entryPointTypes: SetProperty<AssertionsEntryPointType>

    /**
     * Entry point classes inherit from core Assertj classes
     */
    abstract val entryPointInherits: Property<Boolean>

    /**
     * Clean output directory before generating assertions.
     */
    abstract val cleanOutputDir: Property<Boolean>

    /**
     * Generate assertions for non-public fields and properties as well.
     */
    abstract val generateForNonPublicFields: Property<Boolean>

    /**
     * Regexes for classes to be excluded
     */
    abstract val excludes: ListProperty<String>
}
