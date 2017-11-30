package com.github.fhermansson.gradle.assertj.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

open class AssertjGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        project.extensions.create("assertjGenerator", AssertjGeneratorExtension::class.java, project)
        project.tasks.create("generateAssertions", GenerateAssertions::class.java)
    }
}