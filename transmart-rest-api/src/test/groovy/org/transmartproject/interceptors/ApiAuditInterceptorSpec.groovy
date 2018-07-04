package org.transmartproject.interceptors

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */

@TestFor(ApiAuditInterceptor)
class ApiAuditInterceptorSpec extends Specification {

    void "Test arvados interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "arvados", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "save"         | _
        "delete"       | _
        "update"       | _
        "index"        | _
        "anyNewAction" | _
    }

    void "Test concept interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "concept", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "index"        | _
        "show"         | _
        "anyNewAction" | _
    }

    void "Test config interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "config", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "index"        | _
        "update"       | _
        "anyNewAction" | _
    }

    void "Test dimension interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "dimension", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "list"         | _
        "anyNewAction" | _
    }

    void "Test export interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "export", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "createJob"    | _
        "run"          | _
        "cancel"       | _
        "delete"       | _
        "get"          | _
        "download"     | _
        "jobStatus"    | _
        "dataFormats"  | _
        "fileFormats"  | _
        "anyNewAction" | _
    }

    void "Test export interceptor not matching"() {
        when: "A request is made to the listJobs action"
        withRequest(controller: "export", action: 'listJobs')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }

    void "Test patientQuery interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "patientQuery", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action             | _
        "listPatients"     | _
        "findPatient"      | _
        "findPatientSet"   | _
        "findPatientSets"  | _
        "createPatientSet" | _
        "anyNewAction"     | _
    }

    void "Test query interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "query", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action                     | _
        "observations"             | _
        "counts"                   | _
        "countsPerConcept"         | _
        "countsPerStudy"           | _
        "countsPerStudyAndConcept" | _
        "aggregatesPerConcept"     | _
        "crosstable"               | _
        "table"                    | _
        "supportedFields"          | _
        "anyNewAction"             | _
    }

    void "Test studyQuery interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "studyQuery", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action                  | _
        "listStudies"           | _
        "findStudy"             | _
        "findStudyByStudyId"    | _
        "findStudiesByStudyIds" | _
        "anyNewAction"          | _
    }

    void "Test relationType interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "relationType", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "index"        | _
        "anyNewAction" | _
    }

    void "Test storage interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "storage", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "show"         | _
        "save"         | _
        "index"        | _
        "indexStudy"   | _
        "delete"       | _
        "update"       | _
        "anyNewAction" | _
    }

    void "Test storageSystem interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "storageSystem", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "save"         | _
        "delete"       | _
        "update"       | _
        "index"        | _
        "anyNewAction" | _
    }

    void "Test system interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "system", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action                   | _
        "afterDataLoadingUpdate" | _
        "anyNewAction"           | _
    }

    void "Test tree interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "tree", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action          | _
        "index"         | _
        "clearCache"    | _
        "rebuildCache"  | _
        "rebuildStatus" | _
        "anyNewAction"  | _
    }

    void "Test userQuery interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "userQuery", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "index"        | _
        "get"          | _
        "save"         | _
        "update"       | _
        "delete"       | _
        "anyNewAction" | _
    }

    void "Test userQuerySet interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "userQuerySet", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action                   | _
        "scan"                   | _
        "getSetChangesByQueryId" | _
        "anyNewAction"           | _
    }

    void "Test version interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "version", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action         | _
        "index"        | _
        "show"         | _
        "anyNewAction" | _
    }
}

