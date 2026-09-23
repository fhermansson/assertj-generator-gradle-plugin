# AssertJ Assertions Generator Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fgithub%2Ffhermansson%2Fassertj-generator%2Fcom.github.fhermansson.assertj-generator.gradle.plugin%2Fmaven-metadata.xml&label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/com.github.fhermansson.assertj-generator)

A Gradle plugin that generates [AssertJ](https://assertj.github.io/doc/#assertj-core-custom-assertions-generator)
assertion classes for your classes and packages.

The `generateAssertions` task runs after `classes` and before the test compilation tasks,
and generated sources are added to the test source set automatically.

#### Requirements

- Gradle 8.5 or newer, tested 8.5–9.x (users on Gradle 7.x–8.4 should stay on plugin version 1.1.5)
- `jakarta.annotation:jakarta.annotation-api` on the test compile classpath, or a stack
  that includes it (e.g. Spring Boot 3+). See [Generated annotations](#generated-annotations).

#### Quick start

```groovy
plugins {
  id 'com.github.fhermansson.assertj-generator' version '2.0.1'
}

repositories { mavenCentral() }

dependencies {
  testImplementation 'org.assertj:assertj-core:3.27.7'
  testCompileOnly 'jakarta.annotation:jakarta.annotation-api:2.1.1'
}

assertjGenerator {
  classOrPackageNames = ['com.example.model']
}
```

Use any assertj-core version you like in your tests — the version the plugin uses
internally (for generation only) is an implementation detail and does not constrain
yours. Generated assertions compile against your test classpath.

Run `./gradlew test` — assertions are generated, compiled, and available via the generated
entry points, e.g. `import static com.example.model.Assertions.assertThat;`.

#### Generated annotations

Generated classes are annotated `@jakarta.annotation.Generated`. No JDK has shipped the
`javax.annotation` class since Java 11, so the annotation always comes from a dependency —
modern stacks (Jakarta EE 9+, Spring Boot 3+) already have it.

The annotation has `SOURCE` retention: it is needed only to compile the generated code
(`testCompileOnly` is enough) and leaves no trace in bytecode or at runtime.

#### Properties

The `assertjGenerator` extension configures the `generateAssertions` task and provides
defaults for additional tasks of type `com.github.fhermansson.gradle.assertj.plugin.GenerateAssertions`.
All properties support `=` assignment in both Groovy and Kotlin DSL.

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| classOrPackageNames | ListProperty\<String\> | [] | Class or package names to generate assertions for |
| sourceSet | Property\<SourceSet\> | sourceSets.main | The sourceSet containing classes to generate assertions for. The task depends on its `classes` task. |
| testSourceSet | Property\<SourceSet\> | sourceSets.test | The target sourceSet. `outputDir` is added to its srcDirs, and its `compileJava`, `compileKotlin` and `compileGroovy` tasks depend on this task. |
| outputDir | DirectoryProperty | [buildDirectory]/generated/sources/assertj/[testSourceSet.name] | Where to put the generated classes |
| entryPointPackage | Property\<String\> | unset | Destination package for entry point classes. The generator derives it from the generated classes if unset |
| entryPointTypes | SetProperty\<AssertionsEntryPointType\> | ['STANDARD'] | Types of entry point classes to generate. Values: 'STANDARD', 'SOFT', 'BDD', 'JUNIT_SOFT', 'BDD_SOFT', 'JUNIT_BDD_SOFT', 'AUTO_CLOSEABLE_SOFT', 'AUTO_CLOSEABLE_BDD_SOFT' |
| entryPointInherits | Property\<Boolean\> | true | Entry point classes [inherit](https://assertj.github.io/doc/#assertj-core-custom-assertions-entry-point) from core AssertJ classes |
| cleanOutputDir | Property\<Boolean\> | true | Remove all files in `outputDir` before generating assertions |
| excludes | ListProperty\<String\> | [] | Regexes matched against fully qualified class names; matching classes are skipped |
| generateForNonPublicFields | Property\<Boolean\> | false | Generate assertions for non-public fields without public accessors as well |

#### Multiple source sets

For each additional source set, register an extra task. Properties you set on the task
win; unset task properties fall back to the `assertjGenerator` extension, and to plugin
defaults if the extension does not set them either:

```groovy
import com.github.fhermansson.gradle.assertj.plugin.GenerateAssertions

assertjGenerator {
  classOrPackageNames = ['com.example.model']
  entryPointTypes = ['STANDARD', 'SOFT']
}

tasks.register('generateOtherAssertions', GenerateAssertions) {
  classOrPackageNames = ['com.other.model']
  sourceSet = sourceSets.other
  testSourceSet = sourceSets.otherTest
}
```

This also works with source sets from other plugins, e.g. `java-test-fixtures`:

```groovy
tasks.register('generateTestFixturesAssertions', GenerateAssertions) {
  sourceSet = sourceSets.testFixtures
}
```

#### Upgrading from 1.x

Three changes in 2.0.0:

1. The default `outputDir` moved from `src/[testSourceSet.name]/generated-java` to
   `[buildDirectory]/generated/sources/assertj/[testSourceSet.name]`. Generated sources
   now live under the build directory: nothing to commit or .gitignore, and `gradle clean`
   removes them. If you relied on the old location, set it explicitly:

```groovy
assertjGenerator {
  outputDir = file('src/test/generated-java')
}
```

2. Generated classes are now annotated `@jakarta.annotation.Generated` (the
   `useJakartaAnnotations` option is gone — jakarta is always used). See
   [Generated annotations](#generated-annotations).

3. The extension now uses Gradle's lazy `Property` types. `=` assignment works in both
   Groovy and Kotlin DSL; list-typed properties changed from arrays to lists, and
   `outputDir` is now a `DirectoryProperty` that takes files or directory providers.
4. If you have kotlinter lint/format workarounds for the generated directory
   (`exclude { ... }` and `mustRunAfter`/`dependsOn` on `LintTask`/`FormatTask`),
   remove them — they are no longer needed since 2.0.1: the generated sources
   directory is wired as a task output provider, so consuming tasks carry the
   dependency implicitly.
