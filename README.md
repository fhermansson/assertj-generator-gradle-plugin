### Assertj assertions generator gradle plugin

A gradle plugin with task to generate assertions using [AssertJ Assertions Generator](http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html).

The task `generateAssertions` will  run after `compileJava` and before `compileTestJava`


#### Configuration
```groovy
plugins {
  id "com.github.fhermansson.assertj-generator" version "1.0.2"
}

assertjGenerator {
    classOrPackageNames = ['com.example.model']
}
```

##### Properties

| Property | Type | Default | Description |
| -------- | ---- | ------- | ------------|
|classOrPackageNames|String[]|{}|Class or package names you want to generate assertions for|
|entryPointPackage|String|null|Destination package for entry point classes. The generator will choose if null|
|outputDir|File|src/test/generated-java|Where to put the generated classes|
|standard|boolean|true|Generate Assertions entry point class|
|soft|boolean|true|Generate SoftAssertions entry point class|
|bdd|boolean|false|Generate BddAssertions entry point class|
|junit_soft|boolean|false|Generate JunitSoftAssertions entry point class|
|entryPointInherits|boolean|true|Entry point classes [inherit](http://joel-costigliola.github.io/assertj/assertj-core-custom-assertions.html#single-assertion-entry-point) from core Assertj classes|

