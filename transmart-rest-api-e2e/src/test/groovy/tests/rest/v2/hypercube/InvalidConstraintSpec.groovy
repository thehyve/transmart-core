/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2.hypercube

import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static base.ContentTypeFor.contentTypeForProtobuf
import static config.Config.PATH_OBSERVATIONS
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.*
import static tests.rest.v2.constraints.*

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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "

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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue | status
        contentTypeForJSON     | null     | 500
        contentTypeForJSON     | ""       | 500
        contentTypeForJSON     | "  "     | 400
        contentTypeForProtobuf | null     | 500
        contentTypeForProtobuf | ""       | 500
        contentTypeForProtobuf | "  "     | 400
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
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
        acceptType             | badValue
        contentTypeForJSON     | null
        contentTypeForJSON     | ""
        contentTypeForJSON     | "  "
        contentTypeForProtobuf | null
        contentTypeForProtobuf | ""
        contentTypeForProtobuf | "  "
    }

}
