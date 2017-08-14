/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.*
import static tests.rest.constraints.ConceptConstraint

/**
 *  TMPREQ-8 Specifying user/group access by study
 */
@RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
class AccessLevelSpec extends RESTSpec {

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get an access error"
     */
    def "restricted access "(def acceptType) {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]),
                statusCode: 403
        ]


        when: "I try to get a concept from that study"
        def responseData = get(request)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == 'Access denied to concept path: \\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'

        where:
        acceptType | _
        JSON       | _
        PROTOBUF   | _
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get the observations"
     */
    def "unrestricted access"(def acceptType, def newSelector) {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]),
                user      : UNRESTRICTED_USER
        ]


        when: "I try to get a concept from that study"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "I get the observations"
        assert selector.cellCount == 2
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('SCSCP:DEM:AGE')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get the observations"
     */
    def "unrestricted access admin"(def acceptType, def newSelector) {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]),
                user      : ADMIN_USER
        ]


        when: "I try to get a concept from that study"
        def responseData = get(request)

        then: "I get the observations"
        def selector = newSelector(responseData)

        assert selector.cellCount == 2
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('SCSCP:DEM:AGE')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }
}