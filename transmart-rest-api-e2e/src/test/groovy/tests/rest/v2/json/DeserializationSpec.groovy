package tests.rest.v2.json

import base.RESTSpec
import selectors.ObservationSelectorJson

import static config.Config.*
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.Combination
import static tests.rest.v2.constraints.StudyNameConstraint

class DeserializationSpec extends RESTSpec{

    def "reconstruct observations"(){
        def constraintMap = [type: StudyNameConstraint, studyId: CATEGORICAL_VALUES_ID]
        when:
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then:

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "patient", "sexCd", 'String'),
                    'race' : selector.select(it, "patient", "race", 'String'),
                    'age' : selector.select(it, "patient", "age", 'Int'),
                    'studyId' : selector.select(it, "study", "studyId", 'String'),
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
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then:

        def map = []
            map.addAll(CATEGORICAL_VALUES_OBSERVATIONS)
            map.addAll(CLINICAL_TRIAL_OBSERVATIONS)

        (0..<selector.cellCount).each {
            def temp = [
                    'sexCd' : selector.select(it, "patient", "sexCd", 'String'),
                    'race' : selector.select(it, "patient", "race", 'String'),
                    'age' : selector.select(it, "patient", "age", 'Int'),
                    'studyId' : selector.select(it, "study", "studyId", 'String'),
                    'conceptCode' : selector.select(it, "concept", "conceptCode", 'String'),
                    'value' : selector.select(it)
            ]
            assert map.contains(temp)
        }
    }


    def CATEGORICAL_VALUES_OBSERVATIONS = [
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(26), 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Latino', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(24), 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:SEX:F', 'value' : 'Female', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'studyId' : 'CATEGORICAL_VALUES'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(20), 'studyId' : 'CATEGORICAL_VALUES']
    ]

    def CLINICAL_TRIAL_OBSERVATIONS = [
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(26), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(80), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(90), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(88), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(24), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(56), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(57), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(20), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(66), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(68), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(56), 'studyId' : 'CLINICAL_TRIAL'],
            ['sexCd': 'Female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(88), 'studyId' : 'CLINICAL_TRIAL']
    ]

}
