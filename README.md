### Assertj assertions generator gradle plugin

A gradle plugin with task to generate assertions using [AssertJ Assertions Generator](http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html).

The task `generateAssertions` will by default run after `classes` and before `compileTestJava`

#### Requirements
Gradle version >= 7.1
#### Configuration
```groovy
plugins {
  id "com.github.fhermansson.assertj-generator" version "1.1.5"
}

assertjGenerator {
    classOrPackageNames = ['com.example.model']
}
```

The `assertjGenerator` extension configures the `generateAssertions` task, and provides default values for additional 
tasks of 
type `com.github.fhermansson.gradle.assertj.plugin.GenerateAssertions`.

##### Properties

| Property | Type | Default | Description |
| -------- | ---- | ------- | ------------|
|classOrPackageNames|String[]|[]|Class or package names you want to generate assertions for|
|entryPointPackage|String|null|Destination package for entry point classes. The generator will choose if null|
|outputDir|Object|src/[testSourceSet.name]/generated-java|Where to put the generated classes. Will be resolved with project.file(outputDir)|
|sourceSet|SourceSet|sourceSets.main|The sourceSet containing classes that assertions should be generated for. This task will depend on the `classes` task for this sourceSet.|
|testSourceSet|SourceSet|sourceSets.test|The target sourceSet for assertions. `outputDir` will be added to the srcDirs of this sourceSet, and the `compileJava`, `compileKotlin` and `compileGroove` tasks for the sourceSet will depend on this task.|
|entryPointTypes|AssertionsEntryPointType[]|['STANDARD']|Types of entry point classes to generate. Possible values: 'STANDARD', 'SOFT', 'BDD', 'JUNIT_SOFT', 'BDD_SOFT', 'JUNIT_BDD_SOFT', 'AUTO_CLOSEABLE_SOFT', 'AUTO_CLOSEABLE_BDD_SOFT'|
|entryPointInherits|boolean|true|Entry point classes [inherit](http://joel-costigliola.github.io/assertj/assertj-core-custom-assertions.html#single-assertion-entry-point) from core Assertj classes|
|cleanOutputDir|boolean|true|Remove all files in `outputDir` before generating assertions.|
|privateFields|boolean|false|Generate assertions for not public properties and fields|


##### How to create additional tasks
In this example a task `generateOtherAssertions` is added to the project, generating assertions for an other sourceSet.
Both `generateOtherAssertions` and `generateAssertions` will generate `STANDARD` and `SOFT` entry points because 
`generateOtherAssertions` will default to values from the `assertjGenerator` extension.

```groovy
assertjGenerator {
    classOrPackageNames = ['com.example.model']
    entryPointTypes = ['STANDARD', 'SOFT']
}

import com.github.fhermansson.gradle.assertj.plugin.GenerateAssertions

task generateOtherAssertions(type: GenerateAssertions) {
     classOrPackageNames = ['com.other.model']
     sourceSet = sourceSets.other
     testSourceSet = sourceSets.otherTest
 }
```

