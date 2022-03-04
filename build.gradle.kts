plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.16.0"
    id("maven-publish")
    id("org.jmailen.kotlinter") version "3.6.0"
}

group = "com.github.fhermansson"
version = "1.1.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.assertj:assertj-assertions-generator:2.2.1") {
        exclude(module = "logback-classic")
    }
}

gradlePlugin {
    plugins {
        create("assertjGenerator") {
            id = "com.github.fhermansson.assertj-generator"
            implementationClass = "com.github.fhermansson.gradle.assertj.plugin.AssertjGeneratorPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/fhermansson/assertj-generator-gradle-plugin"
    vcsUrl = "https://github.com/fhermansson/assertj-generator-gradle-plugin"
    description = "Generate Assertj assertion classes."
    tags = listOf("code-generation", "assertj", "java")
    (plugins) {
        "assertjGenerator" {
            displayName = "Assertj Generator plugin"
        }
    }
}


tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.2"
}