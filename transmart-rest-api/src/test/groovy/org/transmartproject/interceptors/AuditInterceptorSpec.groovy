package org.transmartproject.interceptors

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */

@TestFor(ConceptAuditInterceptor)
class ConceptAuditInterceptorSpec extends Specification {

    void "Test concept interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "concept", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action  | _
        "index" | _
        "show"  | _
    }

    void "Test concept interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "concept", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(DimensionAuditInterceptor)
class DimensionAuditInterceptorSpec extends Specification {

    void "Test dimension interceptor matching"() {
        when: "A request is made to the list action"
        withRequest(controller: "dimension", action: 'list')

        then: "The interceptor does match"
        interceptor.doesMatch()
    }

    void "Test dimension interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "dimension", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(ExportAuditInterceptor)
class ExportAuditInterceptorSpec extends Specification {

    void "Test export interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "export", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action        | _
        "createJob"   | _
        "run"         | _
        "cancel"      | _
        "delete"      | _
        "get"         | _
        "download"    | _
        "jobStatus"   | _
        "dataFormats" | _
        "fileFormats" | _
    }

    void "Test export interceptor not matching"() {
        when: "A request is made to the listJobs action"
        withRequest(controller: "export", action: 'listJobs')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(PatientQueryAuditInterceptor)
class PatientQueryAuditInterceptorSpec extends Specification {

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
    }

    void "Test patientQuery interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "patientQuery", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(QueryAuditInterceptor)
class QueryAuditInterceptorSpec extends Specification {

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
    }

    void "Test query interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "query", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(StudyQueryAuditInterceptor)
class StudyQueryAuditInterceptorSpec extends Specification {

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
    }

    void "Test studyQuery interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "studyQuery", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(SystemAuditInterceptor)
class SystemAuditInterceptorSpec extends Specification {

    void "Test system interceptor matching"() {
        when:
        "A request is made to the afterDataLoadingUpdate action"
        withRequest(controller: "system", action: 'afterDataLoadingUpdate')

        then: "The interceptor does match"
        interceptor.doesMatch()
    }

    void "Test system interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "system", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(TreeAuditInterceptor)
class TreeAuditInterceptorSpec extends Specification {

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
    }

    void "Test tree interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "tree", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

@TestFor(UserQueryAuditInterceptor)
class UserQueryAuditInterceptorSpec extends Specification {

    void "Test userQuery interceptor matching"() {
        when:
        "A request is made to the $action action"
        withRequest(controller: "userQuery", action: action)

        then: "The interceptor does match"
        interceptor.doesMatch()

        where:
        action   | _
        "index"  | _
        "get"    | _
        "save"   | _
        "update" | _
        "delete" | _
    }

    void "Test userQuery interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "userQuery", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
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
    }

    void "Test userQuerySet interceptor not matching"() {
        when: "A request is made to an invalid action"
        withRequest(controller: "userQuerySet", action: 'invalid')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}

