/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import base.RESTSpec

import static config.Config.PATH_PATIENTS
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*

class CohortSpec extends RESTSpec {


    def "get males that are caucasian the wrong way"(){
        def caucasian = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                        [type: ValueConstraint, valueType: STRING, operator: EQUALS, value:'Caucasian']
                ]
        ]

        def male = [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Male\\"]


        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        caucasian, male
                ]
        ]


        when:
        def responseData = get([path: PATH_PATIENTS, acceptType: contentTypeForJSON, query: toQuery(constraintMap)])

        then: "2 patients are returned"
        assert responseData.patients.size() == 0
    }

    def "get males that are caucasian the right way"(){
        def caucasian = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                        [type: ValueConstraint, valueType: STRING, operator: EQUALS, value:'Caucasian']
                ]
        ]

        when: //get all caucasian
        def responseData = get([path: PATH_PATIENTS, acceptType: contentTypeForJSON, query: toQuery(caucasian)])
        def patientIds = []

        responseData.patients.each {
            patientIds.add(it.id)
        }

        def male = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientIds: patientIds],
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Male\\"]
                ]
        ]

        // get all male  limited by the ids returned from previous query
        responseData = get(path: PATH_PATIENTS, acceptType: contentTypeForJSON, query: toQuery(male))

        then: "1 patient is returned"

        def expected = [age:26, birthDate:null, deathDate:null, id:-60, inTrialId:'1', maritalStatus:null, race:'Caucasian', religion:null, sex:'MALE', trial:'CATEGORICAL_VALUES']
        assert responseData.patients.size() == 1
        assert responseData.patients[0].equals(expected)
    }
}
