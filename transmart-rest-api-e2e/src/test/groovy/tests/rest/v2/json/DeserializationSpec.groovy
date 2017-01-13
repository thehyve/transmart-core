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
                    'sex' : selector.select(it, "patient", "sex", 'String'),
                    'race' : selector.select(it, "patient", "race", 'String'),
                    'age' : selector.select(it, "patient", "age", 'Int'),
                    'study' : selector.select(it, "study", "name", 'String'),
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
                    'sex' : selector.select(it, "patient", "sex", 'String'),
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
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(26), 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:SEX:M', 'value' : 'Male', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Latino', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(24), 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:SEX:F', 'value' : 'Female', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:RACE', 'value' : 'Caucasian', 'study' : 'CATEGORICAL_VALUES'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CV:DEM:AGE', 'value' : new BigDecimal(20), 'study' : 'CATEGORICAL_VALUES']
    ]

    def CLINICAL_TRIAL_OBSERVATIONS = [
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(26), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(80), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(90), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Caucasian', 'age': 26, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(88), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(24), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(56), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'male', 'race' : 'Latino', 'age': 24, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(57), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:DEM:AGE', 'value' : new BigDecimal(20), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(66), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(68), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(56), 'study' : 'CLINICAL_TRIAL'],
            ['sex': 'female', 'race' : 'Caucasian', 'age': 20, 'conceptCode' : 'CT:VSIGN:HR', 'value' : new BigDecimal(88), 'study' : 'CLINICAL_TRIAL']
    ]

}
