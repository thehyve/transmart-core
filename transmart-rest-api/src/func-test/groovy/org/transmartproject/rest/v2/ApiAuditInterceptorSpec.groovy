package org.transmartproject.rest.v2

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.transmartproject.core.log.AccessLogEntry
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.interceptors.ApiAuditInterceptor
import org.transmartproject.mock.MockAccessLogEntryResource
import org.transmartproject.rest.BusinessExceptionController
import spock.lang.Unroll

import static org.springframework.http.HttpMethod.*

@Slf4j
class ApiAuditInterceptorSpec extends V2ResourceSpec {

    @Autowired
    @Qualifier('apiAuditInterceptor')
    ApiAuditInterceptor interceptor

    @Autowired
    BusinessExceptionController businessExceptionController

    @Autowired
    AccessLogEntryResource accessLogEntryResource

    void mocksSetup() {
        assert accessLogEntryResource instanceof MockAccessLogEntryResource
        ((MockAccessLogEntryResource)accessLogEntryResource).entries.clear()
    }

    List<AccessLogEntry> getLogEntries() {
        assert accessLogEntryResource instanceof MockAccessLogEntryResource
        ((MockAccessLogEntryResource)accessLogEntryResource).entries
    }

    @Unroll
    void "Test arvados interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the ${action} action"
        request(method, "${contextPath}/arvados/workflows${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method | action | body
        POST   | ''     | [:]
        DELETE | '/123' | null
        PUT    | '/123' | [:]
        GET    | ''     | null
    }

    @Unroll
    void "Test concept interceptor matching GET #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        get("${contextPath}/concepts${action}")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        _| action
        _| '/123'
        _| ''
    }

    @Unroll
    void "Test config interceptor matching method #method"() {
        given:
        mocksSetup()

        when: "A request is made to the config endpoint"
        request(method, "${contextPath}/config", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method | body
        PUT    | [:]
        GET    | null
    }

    void "Test dimension interceptor matching"() {
        given:
        mocksSetup()

        when: "A request is made to the dimensions endpoint"
        def response = get("${contextPath}/dimensions/patient/elements")
        sleep(1000)

        then: "A log entry is created"
        response.statusCode == HttpStatus.NOT_FOUND
        logEntries.size() == 1
    }

    @Unroll
    void "Test export interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        request(method, "${contextPath}/export${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method | action             | body
        POST   | "/job"             | [:]
        POST   | "/123/run"         | [:]
        POST   | "/123/cancel"      | [:]
        DELETE | "/123"             | null
        GET    | "/123"             | null
        GET    | "/123/download"    | null
        GET    | "/123/status"      | null
        POST   | "/data_formats"    | [:]
        GET    | "/file_formats"    | null
    }

    void "Test export interceptor not matching"() {
        given:
        mocksSetup()

        when: "A request is made to the export jobs endpoint"
        def response = get("${contextPath}/export/jobs")
        sleep(1000)

        then: "No log entry is created"
        response.statusCode == HttpStatus.OK
        logEntries.size() == 0
    }

    @Unroll
    void "Test patientQuery interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        request(method, "${contextPath}${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action                | body
        GET     | "/patients"           | null
        GET     | "/patients/123"       | null
        GET     | "/patient_sets/123"   | null
        GET     | "/patient_sets"       | null
        POST    | "/patient_sets"       | [:]
    }

    @Unroll
    void "Test query interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        request(method, "${contextPath}${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action                                        | body
        POST    | "/observations"                               | [:]
        POST    | "/observations/counts"                        | [contraint: [type: true]]
        POST    | "/observations/counts_per_concept"            | [:]
        POST    | "/observations/counts_per_study"              | [:]
        POST    | "/observations/counts_per_study_and_concept"  | [:]
        POST    | "/observations/aggregates_per_concept"        | [:]
        POST    | "/observations/crosstable"                    | [:]
        POST    | "/observations/table"                         | [:]
        GET     | "/supported_fields"                           | null
    }

    @Unroll
    void "Test studyQuery interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        get("${contextPath}/studies${action}")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        _| action
        _| ""
        _| "/123"
        _| "/studyId/123"
        _| "/studyIds?studyIds=[123,456]"
    }

    void "Test relationType interceptor matching"() {
        given:
        mocksSetup()

        when: "A request is made to the relation types endpoint"
        get("${contextPath}/pedigree/relation_types")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1
    }

    @Unroll
    void "Test storage interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        get("${contextPath}${action}")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action                | body
        GET     | "/files/123"          | null
        POST    | "/files"              | [:]
        GET     | "/files"              | null
        GET     | "/studies/123/files"  | null
        DELETE  | "/files/123"          | null
        PUT     | "/files/123"          | [:]
    }

    @Unroll
    void "Test storageSystem interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        request(method, "${contextPath}${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action            | body
        POST    | "/storage"        | [:]
        DELETE  | "/storage/123"    | null
        PUT     | "/storage/123"    | [:]
        GET     | "/storage"        | null
        GET     | "/storage/123"    | null
    }

    void "Test system interceptor matching"() {
        given:
        mocksSetup()

        when: "A request is made to the system endpoint"
        get("${contextPath}/system/after_data_loading_update")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1
    }

    @Unroll
    void "Test tree interceptor matching GET /tree_nodes/#action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        get("${contextPath}/tree_nodes${action}")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        _| action
        _| ""
        _| "/clear_cache"
        _| "/rebuild_cache"
        _| "/rebuild_status"
    }

    @Unroll
    void "Test userQuery interceptor matching #method #action"() {
        given:
        mocksSetup()

        when:
        "A request is made to the $action action"
        request(method, "${contextPath}/queries${action}", body)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action    | body
        GET     | ""        | null
        GET     | "/123"    | null
        POST    | ""        | [:]
        PUT     | "/123"    | [:]
        DELETE  | "/123"    | null
    }

    @Unroll
    void "Test userQuerySet interceptor matching #method #action"() {
        given:
        mocksSetup()

        when: "A request is made to the $action action"
        request(method, "${contextPath}/queries${action}", null)
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        method  | action
        POST    | "/sets/scan"
        GET     | "/123/sets"
    }

    @Unroll
    void "Test version interceptor matching /versions/#action"() {
        given:
        mocksSetup()

        when:
        "A request is made to the $action action"
        get("${baseUrl}/versions${action}")
        sleep(1000)

        then: "A log entry is created"
        logEntries.size() == 1

        where:
        _| action
        _| ""
        _| "/v2"
    }

}
