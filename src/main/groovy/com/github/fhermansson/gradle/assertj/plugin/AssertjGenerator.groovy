package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.description.ClassDescription
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete

import java.util.stream.Stream

import static java.util.stream.Collectors.toSet

class AssertjGenerator implements Plugin<Project> {

    private static ClassToClassDescriptionConverter classDescriptionConverter = new ClassToClassDescriptionConverter()

    @Override
    void apply(Project project) {
        project.configure(project) {
            apply plugin: 'java'
        }
        project.extensions.create 'assertjGenerator', AssertjGeneratorConfiguration
        defineCleanAssertionsTask(project)
        defineGenerateAssertionsTask(project)
    }

    private void defineGenerateAssertionsTask(Project project) {
        project.task(dependsOn: ['cleanAssertions', 'compileJava'], 'generateAssertions') {
            description = 'Generate Assertj Assertions'
            doFirst {

                AssertjGeneratorConfiguration conf = project.assertjGenerator

                BaseAssertionGenerator baseAssertionGenerator = new BaseAssertionGenerator()
                baseAssertionGenerator.directoryWhereAssertionFilesAreGenerated = new File(project.projectDir, conf.outputDir).getAbsolutePath()

                if (conf.entryPointInherits) {
                    baseAssertionGenerator.register(AssertjTemplates.getStandardTemplate())
                    baseAssertionGenerator.register(AssertjTemplates.getSoftTemplate())
                    baseAssertionGenerator.register(AssertjTemplates.getJunitSoftTemplate())
                    baseAssertionGenerator.register(AssertjTemplates.getBddTemplate())
                }

                def classLoader = new URLClassLoader(project.sourceSets.main.runtimeClasspath.collect { it.toURI().toURL() } as URL[])
                def classes = ClassUtil.collectClasses(classLoader, conf.classOrPackageNames)

                Set<ClassDescription> classDescriptions = classes.stream()
                        .map { classDescriptionConverter.convertToClassDescription(it) }
                        .collect(toSet())

                def generated = classDescriptions.stream()
                        .map { baseAssertionGenerator.generateCustomAssertionFor(it) }
                        .collect(toSet())

                def entryPoints = Collections.emptySet()

                if (!generated.isEmpty()) {
                    entryPoints = Stream.of(AssertionsEntryPointType.values())
                            .filter { conf[it.name().toLowerCase()] }
                            .map { baseAssertionGenerator.generateAssertionsEntryPointClassFor(classDescriptions, it, conf.entryPointPackage) }
                            .collect(toSet())
                }


                logger.lifecycle "Generated ${generated.size()} assertion classes, ${entryPoints.size()} entry point classes in ${conf.outputDir}"
            }
        }

        project.compileTestJava.dependsOn('generateAssertions')
        project.sourceSets.test.java.srcDirs += project.assertjGenerator.outputDir
    }

    private void defineCleanAssertionsTask(Project project) {
        project.task(type: Delete, 'cleanAssertions') {
            doLast() {
                this.clean(project)
            }
        }

        project.clean.doFirst {
            this.clean(project)
        }
    }

    private void clean(Project project) {
        project.delete project.assertjGenerator.outputDir
    }
}
