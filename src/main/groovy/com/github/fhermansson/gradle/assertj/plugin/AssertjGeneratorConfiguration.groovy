package com.github.fhermansson.gradle.assertj.plugin

class AssertjGeneratorConfiguration {
    /**
     * Classes and packages to generate assertions for
     */
    String[] classOrPackageNames = {}
    /**
     * Destination package for entry poit classes. The generator will choose if null
     */
    String entryPointPackage
    /**
     * Output directory for generated classes
     */
    File outputDir = new File('src/test/generated-java')
    /**
     * Generate Assertions entry point class
     */
    boolean standard = true
    /**
     * Generate SoftAssertions entry point class
     */
    boolean soft = true
    /**
     * Generate BddAssertions entry point class
     */
    boolean bdd = false
    /**
     * Generate JunitSoftAssertions entry point class
     */
    boolean junit_soft = false
    /**
     * Entry point classes inherit from core Assertj classes
     */
    boolean entryPointInherits = true
}

