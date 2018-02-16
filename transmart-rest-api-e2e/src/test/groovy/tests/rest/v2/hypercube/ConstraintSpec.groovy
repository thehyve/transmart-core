/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import base.RESTSpec
import spock.lang.Requires

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.*
import static org.hamcrest.Matchers.is
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.Operator.*
import static tests.rest.ValueType.*
import static tests.rest.constraints.*

class ConstraintSpec extends RESTSpec {

    /**
     * TrueConstraint.class,
     BiomarkerConstraint.class,
     ModifierConstraint.class,
     FieldConstraint.class,
     ValueConstraint.class,
     TimeConstraint.class,
     PatientSetConstraint.class,
     Negation.class,
     Combination.class,
     TemporalConstraint.class,
     ConceptConstraint.class,
     StudyNameConstraint.class,
     NullConstraint.class
     */
    def final INVALIDARGUMENTEXCEPTION = "InvalidArgumentsException"
    def final CONSTRAINTBINDINGEXCEPTION = "ConstraintBindingException"
    def final EMPTYCONTSTRAINT = "Empty constraint parameter."

    /**
     *  when:" I do a Get query/observations with a wrong type."
     *  then: "then I get a 400 with 'Constraint not supported: BadType.'"
     */
    def "Get /query/observations malformed query"() {
        when: " I do a Get query/observations with a wrong type."
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: 'BadType']),
                statusCode: 400
        ]

        def responseData = get(request)

        then: "then I get a 400 with 'Constraint not supported: BadType.'"
        that responseData.httpStatus, is(400)
        that responseData.type, is(CONSTRAINTBINDINGEXCEPTION)
        that responseData.message, is('Constraint not supported: BadType.')

        where:
        acceptType | _
        JSON       | _
        PROTOBUF   | _
    }

    @Requires({ RUN_HUGE_TESTS })
    def "TrueConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: TrueConstraint])
        ]

        when:
        def responseData = get(request)

        then:
        def selector = newSelector(responseData)

        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "ModifierConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ])
        ]

        when:
        def responseData = get(request)

        then:
        def selector = newSelector(responseData)

        assert selector.cellCount == 8
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "concept", "conceptCode", 'String'))
            assert selector.select(it) != null
        }

        when:
        request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type  : ModifierConstraint, modifierCode: "TNS:SMPL",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ])
        ]

        responseData = get(request)
        selector = newSelector(responseData)

        then:
        assert selector.cellCount == 8
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "concept", "conceptCode", 'String'))
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
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
                                     value   : 30])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "patient", "age", 'Int') < 100
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "ValueConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 176])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it) > 176
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "TimeConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type    : TimeConstraint,
                                     field   : [dimension: 'start time', fieldName: 'startDate', type: DATE],
                                     operator: AFTER,
                                     values  : [toDateString("01-01-2016Z")]])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String') != ''
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "PatientSetConstraint.class"() {
        def postRequest = [
                path          : PATH_PATIENT_SET,
                acceptType    : JSON,
                'Content-Type': JSON,
                query         : [name: 'test_PatientSetConstraint'],
                body          : toJSON([type: PatientSetConstraint, patientIds: [-62]]),
                statusCode    : 201

        ]
        def setID = post(postRequest)

        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: PatientSetConstraint, patientSetId: setID.id])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String') != ''
        }

        when:
        request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: PatientSetConstraint, patientIds: [-62]])
        ]
        responseData = get(request)
        selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String') != ''
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    @Requires({ RUN_HUGE_TESTS })
    def "Negation.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type: Negation,
                        arg : [type: PatientSetConstraint, patientIds: [-62, -52, -42]]
                ])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert !selector.select(it, "study", "name", 'String').equals('EHR')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "Combination.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: PatientSetConstraint, patientSetId: 0, patientIds: [-62]],
                                [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                        ]
                ])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('EHR:VSIGN:HR')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "TemporalConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type           : TemporalConstraint,
                        operator       : AFTER,
                        eventConstraint: [
                                type     : ValueConstraint,
                                valueType: NUMERIC,
                                operator : LESS_THAN,
                                value    : 60
                        ]
                ])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)


        then:
        HashSet conceptCodes = []
        (0..<selector.cellCount).each {
            conceptCodes.add selector.select(it, "concept", "conceptCode", 'String')
        }
        assert conceptCodes.containsAll("EHR:VSIGN:HR", "EHRHD:VSIGN:HR", "EHRHD:HD:EXPLUNG", "EHRHD:HD:EXPBREAST")

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "ConceptConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('EHR:VSIGN:HR')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "StudyConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: StudyNameConstraint, studyId: EHR_ID])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "study", "name", 'String').equals('EHR')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    @Requires({ RUN_HUGE_TESTS })
    def "NullConstraint.class"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type : NullConstraint,
                        field: [dimension: 'end time', fieldName: 'endDate', type: DATE]
                ])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        HashSet conceptCodes = []
        (0..<selector.cellCount).each {
            conceptCodes.add(selector.select(it, "concept", "conceptCode", 'String'))
        }
        assert conceptCodes.containsAll(['CV:DEM:SEX:M', 'CV:DEM:SEX:F', 'CV:DEM:RACE', 'CV:DEM:AGE'])

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    def "SubSelectionConstraint.class"() {
        when:
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type      : SubSelectionConstraint,
                        dimension : 'patient',
                        constraint: [type    : FieldConstraint,
                                     field   : [dimension: 'patient',
                                                fieldName: 'age',
                                                type     : NUMERIC],
                                     operator: EQUALS,
                                     value   : 30],
                ])
        ]

        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert selector.select(it, "patient", "age", 'Int') == 30
        }

        when:
        request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type      : SubSelectionConstraint,
                        dimension : 'visit',
                        constraint: [type: 'and',
                                     args: [[
                                                    type     : ValueConstraint,
                                                    valueType: NUMERIC,
                                                    operator : EQUALS,
                                                    value    : 59.0,
                                            ], [
                                                    type   : StudyNameConstraint,
                                                    studyId: "EHR",
                                            ]
                                     ]
                        ]
                ])
        ]

        responseData = get(request)
        selector = newSelector(responseData)
        def visits = (0..<selector.cellCount).collect { selector.select(it, "visit", "encounterNum", "Double") } as Set

        then:
        selector.cellCount == 2
        // ensure we are also finding other cells than the value we specified in the constraint
        (0..<selector.cellCount).collect { selector.select(it) }.any { it != 59.0 }
        visits.size() == 1

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

}
