package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet

open class AssertjGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        val extension =
            project.extensions.create("assertjGenerator", AssertjGeneratorExtension::class.java)
        val sourceSets =
            project.extensions
                .getByType(JavaPluginExtension::class.java)
                .sourceSets
        extension.sourceSet.convention(sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME))
        extension.testSourceSet.convention(sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME))
        extension.entryPointTypes.convention(setOf(AssertionsEntryPointType.STANDARD))
        extension.entryPointInherits.convention(true)
        extension.cleanOutputDir.convention(true)
        extension.generateForNonPublicFields.convention(false)

        project.tasks.register("generateAssertions", GenerateAssertions::class.java)

        /*
         * After the build script has run, every GenerateAssertions task (the built-in
         * one and custom ones) gets its extension defaults linked and is wired into the
         * build. The hook is needed because source sets can be reassigned in the script,
         * and srcDir/dependsOn wiring must react to the final configuration.
         */
        project.afterEvaluate {
            project.tasks.withType(GenerateAssertions::class.java).forEach { wire(it, extension) }
        }
    }

    private fun wire(
        task: GenerateAssertions,
        defaults: AssertjGeneratorExtension,
    ) {
        /*
         * Live provider links to the extension values; an explicit value set on the task
         * always wins. The collection links are unconditional because Gradle gives
         * decorated ListProperty/SetProperty an implicit empty convention — an isPresent
         * guard would never fire and the empty value would shadow the extension.
         */
        task.classOrPackageNames.convention(defaults.classOrPackageNames)
        task.excludes.convention(defaults.excludes)
        task.entryPointTypes.convention(defaults.entryPointTypes)
        if (!task.entryPointPackage.isPresent) task.entryPointPackage.convention(defaults.entryPointPackage)
        if (!task.entryPointInherits.isPresent) task.entryPointInherits.convention(defaults.entryPointInherits)
        if (!task.cleanOutputDir.isPresent) task.cleanOutputDir.convention(defaults.cleanOutputDir)
        if (!task.generateForNonPublicFields.isPresent) {
            task.generateForNonPublicFields.convention(defaults.generateForNonPublicFields)
        }
        /*
         * outputDir is a three-level fallback: an explicit task value wins (Gradle
         * ignores conventions once a value is set), then the extension's outputDir
         * (no-op if the user never set it), then the plugin default below. The link
         * needs no isPresent guard: convention() on a property that already holds a
         * value is simply ignored.
         */
        task.outputDir.convention(defaults.outputDir)

        val sourceSet =
            task.sourceSet
                ?: throw IllegalStateException("assertjGenerator: no sourceSet configured")
        val testSourceSet =
            task.testSourceSet
                ?: throw IllegalStateException("assertjGenerator: no testSourceSet configured")

        if (!task.outputDir.isPresent) {
            task.outputDir.convention(
                task.project.layout.buildDirectory.dir(
                    "generated/sources/assertj/${testSourceSet.name}",
                ),
            )
        }
        task.classPath.from(sourceSet.runtimeClasspath)
        task.dependsOn(sourceSet.classesTaskName)
        // Wire the generated directory as a task-output provider: any task that
        // reads the source set (compile tasks, kotlinter lint/format, ...) then
        // carries the producer dependency implicitly.
        testSourceSet.java.srcDir(task.outputDir)
        listOf("java", "kotlin", "groovy").forEach { lang ->
            task.project.tasks
                .matching { it.name == testSourceSet.getCompileTaskName(lang) }
                .configureEach { dependsOn(task) }
        }
    }
}
