package tests.rest.v2.json

import base.RESTSpec
import selectors.protobuf.ObservationSelectorJson

import static config.Config.*
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.Combination
import static tests.rest.v2.constraints.StudyConstraint

class DeserializationSpec extends RESTSpec{

    def "reconstruct observations"(){
        def constraintMap = [type: StudyConstraint, studyId: CATEGORICAL_VALUES_ID]
        when:
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then:

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
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then:

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
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:AGE', 'value' : 26.0, 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Latino', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:AGE', 'value' : 24.0, 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:SEX:F', 'value' : 'Female', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:AGE', 'value' : 20.0, 'studyId' : 'CATEGORICAL_VALUES']
    ]

    def CLINICAL_TRIAL_OBSERVATIONS = [
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:DEM:AGE', 'value' : 26.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 80.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 90.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:DEM:AGE', 'value' : 24.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'M', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 57.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:DEM:AGE', 'value' : 20.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 66.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 68.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 56.0, 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'F', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : 88.0, 'studyId' : 'CLINICAL_TRIAL']
    ]

}
