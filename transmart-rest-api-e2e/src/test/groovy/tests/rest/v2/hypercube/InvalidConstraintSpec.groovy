/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.PATH_OBSERVATIONS
import static tests.rest.Operator.*
import static tests.rest.ValueType.*
import static tests.rest.constraints.*

class InvalidConstraintSpec extends RESTSpec {

    def "ModifierConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type  : ModifierConstraint, path: badValue,
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        when:
        request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type  : ModifierConstraint, modifierCode: badValue,
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
                statusCode: 400
        ]

        responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "

    }

    def "FieldConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type    : FieldConstraint,
                                     field   : [dimension: 'patient',
                                                fieldName: 'age',
                                                type     : NUMERIC],
                                     operator: EQUALS,
                                     value   : badValue]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "ValueConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: badValue]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "TimeConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type    : TimeConstraint,
                                     field   : [dimension: 'start time', fieldName: 'startDate', type: DATE],
                                     operator: AFTER,
                                     values  : [badValue]]),
                statusCode: status
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == status

        where:
        acceptType | badValue | status
        JSON       | null     | 500
        JSON       | ""       | 500
        JSON       | "  "     | 400
        PROTOBUF   | null     | 500
        PROTOBUF   | ""       | 500
        PROTOBUF   | "  "     | 400
    }

    def "Negation.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type: Negation,
                        arg : badValue
                ]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == 400
        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "Combination.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                badValue,
                                badValue
                        ]
                ]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == 400

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "TemporalConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type           : TemporalConstraint,
                        operator       : AFTER,
                        eventConstraint: [
                                badValue
                        ]
                ]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == 400

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "ConceptConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: badValue]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "StudyConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: StudyNameConstraint, studyId: badValue]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.errors.size() > 0

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

    def "NullConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type : NullConstraint,
                        field: badValue
                ]),
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == 400

        where:
        acceptType | badValue
        JSON       | null
        JSON       | ""
        JSON       | "  "
        PROTOBUF   | null
        PROTOBUF   | ""
        PROTOBUF   | "  "
    }

}
