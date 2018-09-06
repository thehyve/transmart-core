/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec
import spock.lang.Unroll

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.*
import static tests.rest.Operator.OR
import static tests.rest.constraints.Combination
import static tests.rest.constraints.StudyNameConstraint

@RequiresStudy(CATEGORICAL_VALUES_ID)
class DeserializationSpec extends RESTSpec {

    @Unroll
    def "reconstruct observations for #acceptType"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: StudyNameConstraint, studyId: CATEGORICAL_VALUES_ID])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:

        (0..<selector.cellCount).each {
            def temp = [
                    'sex'        : selector.select(it, "patient", "sex", 'String'),
                    'race'       : selector.select(it, "patient", "race", 'String'),
                    'age'        : selector.select(it, "patient", "age", 'Int'),
                    'study'      : selector.select(it, "study", "name", 'String'),
                    'conceptCode': selector.select(it, "concept", "conceptCode", 'String'),
                    'value'      : selector.select(it)
            ]
            assert result.contains(temp)
        }

        where:
        acceptType | newSelector      | result
        JSON       | jsonSelector     | CATEGORICAL_VALUES_OBSERVATIONS_json
        PROTOBUF   | protobufSelector | CATEGORICAL_VALUES_OBSERVATIONS_proto
    }

    @RequiresStudy(CLINICAL_TRIAL_ID)
    @Unroll
    def "reconstruct observations multi study for #acceptType"() {
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: StudyNameConstraint, studyId: CATEGORICAL_VALUES_ID],
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID]
                        ]
                ])
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:

        def map = []
        map.addAll(map1)
        map.addAll(map2)

        (0..<selector.cellCount).each {
            def temp = [
                    'sex'        : selector.select(it, "patient", "sex", 'String'),
                    'race'       : selector.select(it, "patient", "race", 'String'),
                    'age'        : selector.select(it, "patient", "age", 'Int'),
                    'study'      : selector.select(it, "study", "name", 'String'),
                    'conceptCode': selector.select(it, "concept", "conceptCode", 'String'),
                    'value'      : selector.select(it)
            ]
            assert map.contains(temp)
        }

        where:
        acceptType | newSelector      | map1                                  | map2
        JSON       | jsonSelector     | CATEGORICAL_VALUES_OBSERVATIONS_json  | CLINICAL_TRIAL_OBSERVATIONS_json
        PROTOBUF   | protobufSelector | CATEGORICAL_VALUES_OBSERVATIONS_proto | CLINICAL_TRIAL_OBSERVATIONS_proto
    }


    static def CATEGORICAL_VALUES_OBSERVATIONS_json = [
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CV:DEM:SEX:M', 'value': 'Male', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CV:DEM:RACE', 'value': 'Caucasian', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CV:DEM:AGE', 'value': 26.0G, 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CV:DEM:SEX:M', 'value': 'Male', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CV:DEM:RACE', 'value': 'Latino', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CV:DEM:AGE', 'value': 24.0G, 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CV:DEM:SEX:F', 'value': 'Female', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CV:DEM:RACE', 'value': 'Caucasian', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CV:DEM:AGE', 'value': 20.0G, 'study': 'CATEGORICAL_VALUES']
    ]

    static def CLINICAL_TRIAL_OBSERVATIONS_json = [
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CT:DEM:AGE', 'value': 26.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CT:VSIGN:HR', 'value': 80.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CT:VSIGN:HR', 'value': 90.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26, 'conceptCode': 'CT:VSIGN:HR', 'value': 88.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CT:DEM:AGE', 'value': 24.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CT:VSIGN:HR', 'value': 56.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24, 'conceptCode': 'CT:VSIGN:HR', 'value': 57.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CT:DEM:AGE', 'value': 20.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CT:VSIGN:HR', 'value': 66.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CT:VSIGN:HR', 'value': 68.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CT:VSIGN:HR', 'value': 56.0G, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20, 'conceptCode': 'CT:VSIGN:HR', 'value': 88.0G, 'study': 'CLINICAL_TRIAL']
    ]

    static def CATEGORICAL_VALUES_OBSERVATIONS_proto = [
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CV:DEM:SEX:M', 'value': 'Male', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CV:DEM:RACE', 'value': 'Caucasian', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CV:DEM:AGE', 'value': 26.0D, 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CV:DEM:SEX:M', 'value': 'Male', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CV:DEM:RACE', 'value': 'Latino', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CV:DEM:AGE', 'value': 24.0D, 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CV:DEM:SEX:F', 'value': 'Female', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CV:DEM:RACE', 'value': 'Caucasian', 'study': 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CV:DEM:AGE', 'value': 20.0D, 'study': 'CATEGORICAL_VALUES']
    ]

    static def CLINICAL_TRIAL_OBSERVATIONS_proto = [
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CT:DEM:AGE', 'value': 26.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CT:VSIGN:HR', 'value': 80.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CT:VSIGN:HR', 'value': 90.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Caucasian', 'age': 26L, 'conceptCode': 'CT:VSIGN:HR', 'value': 88.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CT:DEM:AGE', 'value': 24.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CT:VSIGN:HR', 'value': 56.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race': 'Latino', 'age': 24L, 'conceptCode': 'CT:VSIGN:HR', 'value': 57.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CT:DEM:AGE', 'value': 20.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CT:VSIGN:HR', 'value': 66.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CT:VSIGN:HR', 'value': 68.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CT:VSIGN:HR', 'value': 56.0D, 'study': 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race': 'Caucasian', 'age': 20L, 'conceptCode': 'CT:VSIGN:HR', 'value': 88.0D, 'study': 'CLINICAL_TRIAL']
    ]

}
