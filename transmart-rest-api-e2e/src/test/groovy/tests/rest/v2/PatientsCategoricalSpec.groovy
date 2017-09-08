/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.CATEGORICAL_VALUES_ID
import static config.Config.PATH_PATIENTS
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.Operator.AND
import static tests.rest.Operator.EQUALS
import static tests.rest.ValueType.STRING
import static tests.rest.constraints.*

/**
 *  TMPREQ-18
 *      enabling fetching patients and observations where a categorical variable has a certain value.
 *      E.g., fetching data for patients with value 'female' for 'Sex' or with value 'Unknown' for 'Diagnosis'.
 */
@RequiresStudy(CATEGORICAL_VALUES_ID)
class PatientsCategoricalSpec extends RESTSpec {

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all patients from the study that have concept Gender"
     *  then: "no patients are returned"
     */
    def "get patient using old data format new style query"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\"])
        ]

        when: "I get all patients from the  study that have concept Gender"
        def responseData = get(request).patients

        then: "no patients are returned"
        that responseData.size(), is(0)
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all patients from the study that have concept Gender\Female"
     *  then: "1 patient is returned"
     */
    def "get patient using old data format old style query"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Female\\"])
        ]

        when: "I get all patients from the study that have concept Female"
        def responseData = get(request).patients

        then: "1 patient is returned"
        that responseData.size(), is(1)
        that responseData, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all patients from the study that have concept Race"
     *  then: "2 patients are returned"
     */
    def "get patient using new data format new style query"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"])
        ]

        when: "I get all patients from the study that have concept Race"
        def responseData = get(request).patients

        then: "3 patients are returned"
        that responseData.size(), is(3)
        that responseData, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all patients from the study that have concept Race with value Caucasian"
     *  then: "2 patients are returned"
     */
    def "get patient using new data format new style query with value"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                                [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: 'Caucasian']
                        ]
                ])
        ]

        when: "I get all patients from the study that have concept Race with value Caucasian"
        def responseData = get(request).patients

        then: "2 patients are returned"
        that responseData.size(), is(2)
        that responseData, everyItem(hasKey('id'))
        that responseData, everyItem(hasEntry('race', 'Caucasian'))
    }

}
