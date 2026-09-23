import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.2.1"
    id("maven-publish")
    id("org.gradle.plugin-compatibility") version "1.1.0"
    id("org.jmailen.kotlinter") version "5.7.0"
    id("pl.allegro.tech.build.axion-release") version "1.21.4"
}

group = "com.github.fhermansson"

scmVersion {
    // Existing tags are "v1.0.2", "v1.1.0", ... — prefix "v" with no separator,
    // so new releases keep tagging as v<version>.
    tag {
        prefix.set("v")
    }
    ignoreUncommittedChanges = false
    releaseOnlyOnReleaseBranches = true
}

version = scmVersion.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.assertj:assertj-assertions-generator:2.2.1") {
        exclude(module = "logback-classic")
    }
    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    website = "https://github.com/fhermansson/assertj-generator-gradle-plugin"
    vcsUrl = "https://github.com/fhermansson/assertj-generator-gradle-plugin"
    plugins {
        create("assertjGenerator") {
            id = "com.github.fhermansson.assertj-generator"
            implementationClass = "com.github.fhermansson.gradle.assertj.plugin.AssertjGeneratorPlugin"
            displayName = "Assertj Generator plugin"
            description = "Generate Assertj assertion classes."
            tags = listOf("code-generation", "assertj", "java")
            compatibility {
                features {
                    // Verified by the functional suite: CC store/reuse test and
                    // FROM-CACHE build cache test.
                    configurationCache = true
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "9.7.1"
}
