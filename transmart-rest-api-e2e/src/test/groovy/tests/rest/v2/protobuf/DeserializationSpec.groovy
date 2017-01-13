package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector

import static config.Config.*
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.Combination
import static tests.rest.v2.constraints.StudyNameConstraint

class DeserializationSpec extends RESTSpec{

    def "reconstruct observations"(){
        def constraintMap = [type: StudyNameConstraint, studyId: CATEGORICAL_VALUES_ID]
        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then:
        ObservationSelector selector = new ObservationSelector(responseData)

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "patient", "sexCd", 'String'),
                    'race' : selector.select(it, "patient", "race", 'String'),
                    'age' : selector.select(it, "patient", "age", 'Int'),
                    'studyId' : selector.select(it, "study", "name", 'String'),
                    'conceptCode' : selector.select(it, "concept", "conceptCode", 'String'),
                    'value' : selector.select(it)
            ]
            assert CATEGORICAL_VALUES_OBSERVATIONS.contains(temp)
        }
    }

    def "reconstruct observations multi study"(){
        def constraintMap = [
                type: Combination,
                operator: OR,
                args: [
                        [type: StudyNameConstraint, studyId: CATEGORICAL_VALUES_ID],
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID]
                ]
        ]

        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then:
        ObservationSelector selector = new ObservationSelector(responseData)

        def map = []
            map.addAll(CATEGORICAL_VALUES_OBSERVATIONS)
            map.addAll(CLINICAL_TRIAL_OBSERVATIONS)

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "patient", "sexCd", 'String'),
                    'race' : selector.select(it, "patient", "race", 'String'),
                    'age' : selector.select(it, "patient", "age", 'Int'),
                    'study' : selector.select(it, "study", "name", 'String'),
                    'conceptCode' : selector.select(it, "concept", "conceptCode", 'String'),
                    'value' : selector.select(it)
            ]
            assert map.contains(temp)
        }
    }


    def CATEGORICAL_VALUES_OBSERVATIONS = [
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 26.0D, 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Latino', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 24.0D, 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:SEX:F', 'value' : 'Female', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'study' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 20.0D, 'study' : 'CATEGORICAL_VALUES']
    ]

    def CLINICAL_TRIAL_OBSERVATIONS = [
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 26.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 80.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 90.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 24.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'male', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 57.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 20.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 66.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 68.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0D, 'study' : 'CLINICAL_TRIAL'],
            ['sexCd': 'female', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0D, 'study' : 'CLINICAL_TRIAL']
    ]

}
