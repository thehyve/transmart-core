/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.CATEGORICAL_VALUES_ID
import static config.Config.PATH_PATIENTS
import static tests.rest.Operator.AND
import static tests.rest.Operator.EQUALS
import static tests.rest.ValueType.STRING
import static tests.rest.constraints.*

@RequiresStudy(CATEGORICAL_VALUES_ID)
class CohortSpec extends RESTSpec {

    def "get males that are caucasian the wrong way"() {
        def caucasian = [
                type    : Combination,
                operator: AND,
                args    : [
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                        [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: 'Caucasian']
                ]
        ]

        def male = [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Male\\"]


        def constraintMap = [
                type    : Combination,
                operator: AND,
                args    : [
                        caucasian, male
                ]
        ]

        when:
        def responseData = get([path: PATH_PATIENTS, acceptType: JSON, query: toQuery(constraintMap)])

        then: "2 patients are returned"
        assert responseData.patients.size() == 0
    }

    def "get males that are caucasian the right way"() {
        def caucasian = [
                type    : Combination,
                operator: AND,
                args    : [
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                        [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: 'Caucasian']
                ]
        ]

        when: //get all caucasian
        def responseData = get([path: PATH_PATIENTS, acceptType: JSON, query: toQuery(caucasian)])
        def patientIds = []

        responseData.patients.each {
            patientIds.add(it.id)
        }

        def male = [
                type    : Combination,
                operator: AND,
                args    : [
                        [type: PatientSetConstraint, patientIds: patientIds],
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Male\\"]
                ]
        ]

        // get all male  limited by the ids returned from previous query
        responseData = get(path: PATH_PATIENTS, acceptType: JSON, query: toQuery(male))

        then: "1 patient is returned"

        responseData.patients.size() == 1
        responseData.patients[0].subjectIds['SUBJ_ID'] == 'CV:60'
    }
}
