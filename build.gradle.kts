plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.2.1"
    id("maven-publish")
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

// The generator declares assertj-core as a range [3.15.0, 3.99.0], which resolves
// to 3.27.x and pulls byte-buddy 1.18.x with Java 24 classfiles. Gradle < 8.5
// cannot instrument such jars when loading the plugin classpath in consumer builds,
// so the range is pinned to a version whose byte-buddy is Java 8 compatible. The
// generator only exercises stable, long-standing assertj-core APIs (Assertions
// entry points), so this pin does not constrain functionality.


// The generator declares its assertj-core dependency as a range [3.15.0, 3.99.0],
// which resolves to 3.27.x and pulls byte-buddy 1.18.x with Java 24 classfiles.
// Gradle < 8.5 cannot instrument such jars when loading the plugin classpath in
// consumer builds. A direct, pinned assertj-core dependency is published in the
// POM and wins resolution over the range. The generator only exercises stable,
// long-standing assertj-core APIs (Assertions entry points), so this pin does
// not constrain functionality.
dependencies {
    implementation("org.assertj:assertj-assertions-generator:2.2.1") {
        exclude(module = "logback-classic")
    }
    implementation("org.assertj:assertj-core:3.24.2")
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
