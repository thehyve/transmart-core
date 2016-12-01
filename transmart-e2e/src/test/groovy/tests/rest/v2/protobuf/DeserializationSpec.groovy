package tests.rest.v2.protobuf

import base.RESTSpec
import protobuf.ObservationsMessageProto
import selectors.protobuf.ObservationSelector

import static config.Config.*
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.Combination
import static tests.rest.v2.constraints.StudyConstraint

class DeserializationSpec extends RESTSpec{

    def "reconstruct observations"(){
        def constraintMap = [type: StudyConstraint, studyId: CATEGORICAL_VALUES_ID]
        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_HYPERCUBE, toQuery(constraintMap))

        then:
        ObservationSelector selector = new ObservationSelector(responseData)

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "PatientDimension", "sexCd", 'String'),
                    'race' : selector.select(it, "PatientDimension", "race", 'String'),
                    'age' : selector.select(it, "PatientDimension", "age", 'Int'),
                    'studyId' : selector.select(it, "StudyDimension", "studyId", 'String'),
                    'conceptCode' : selector.select(it, "ConceptDimension", "conceptCode", 'String'),
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
                        [type: StudyConstraint, studyId: CATEGORICAL_VALUES_ID],
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID]
                ]
        ]

        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_HYPERCUBE, toQuery(constraintMap))

        then:
        ObservationSelector selector = new ObservationSelector(responseData)

        def map = []
            map.addAll(CATEGORICAL_VALUES_OBSERVATIONS)
            map.addAll(CLINICAL_TRIAL_OBSERVATIONS)

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "PatientDimension", "sexCd", 'String'),
                    'race' : selector.select(it, "PatientDimension", "race", 'String'),
                    'age' : selector.select(it, "PatientDimension", "age", 'Int'),
                    'studyId' : selector.select(it, "StudyDimension", "studyId", 'String'),
                    'conceptCode' : selector.select(it, "ConceptDimension", "conceptCode", 'String'),
                    'value' : selector.select(it)
            ]
            assert map.contains(temp)
        }
    }


    def CATEGORICAL_VALUES_OBSERVATIONS = [
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 26.0D, 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Latino', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 24.0D, 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:SEX:F', 'value' : 'Female', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CV:DEM:AGE', 'value' : 20.0D, 'studyId' : 'CATEGORICAL_VALUES']
    ]

    def CLINICAL_TRIAL_OBSERVATIONS = [
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 26.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 80.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 90.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 24.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 57.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:DEM:AGE', 'value' : 20.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 66.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 68.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0D, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20L, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0D, 'studyId' : 'CLINICAL_TRIAL']
    ]

}
