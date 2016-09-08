package com.github.fhermansson.gradle.assertj.plugin

import org.assertj.assertions.generator.Template

class AssertjTemplates {

    private static String readTemplate(String fileName) {
        return AssertjTemplates.classLoader.getResource(fileName).text
    }

    public static Template getStandardTemplate() {
        return new Template(Template.Type.ASSERTIONS_ENTRY_POINT_CLASS, readTemplate('standard_assertions_entry_point_class.txt'));
    }

    public static Template getSoftTemplate() {
        return new Template(Template.Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS, readTemplate('soft_assertions_entry_point_class.txt'));
    }

    public static Template getJunitSoftTemplate() {
        return new Template(Template.Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS, readTemplate('junit_soft_assertions_entry_point_class.txt'));
    }

    public static Template getBddTemplate() {
        return new Template(Template.Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS, readTemplate('bdd_assertions_entry_point_class.txt'));
    }
}
