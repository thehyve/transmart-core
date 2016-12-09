package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*
import static config.Config.*

/**
 *  TMPREQ-18
 *      enabling fetching patients and observations where a categorical variable has a certain value.
 *      E.g., fetching data for patients with value 'female' for 'Sex' or with value 'Unknown' for 'Diagnosis'.
 */
class PatientsCategoricalSpec extends RESTSpec{

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all patients from the study that have concept Gender"
     *  then: "no patients are returned"
     */
    @Requires({CATEGORICAL_VALUES_LOADED})
    def "get patient using old data format new style query"(){
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"

        when: "I get all patients from the  study that have concept Gender"
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\"]

        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap)).patients

        then: "no patients are returned"
        that responseData.size(), is(0)
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all patients from the study that have concept Gender\Female"
     *  then: "1 patient is returned"
     */
    @Requires({CATEGORICAL_VALUES_LOADED})
    def "get patient using old data format old style query"(){
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"

        when: "I get all patients from the study that have concept Female"
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Female\\"]

        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap)).patients

        then: "1 patient is returned"
        that responseData.size(), is(1)
        that responseData, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all patients from the study that have concept Race"
     *  then: "2 patients are returned"
     */
    @Requires({CATEGORICAL_VALUES_LOADED})
    def "get patient using new data format new style query"(){
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"

        when: "I get all patients from the study that have concept Race"
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"]

        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap)).patients

        then: "3 patients are returned"
        that responseData.size(), is(3)
        that responseData, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all patients from the study that have concept Race with value Caucasian"
     *  then: "2 patients are returned"
     */
    @Requires({CATEGORICAL_VALUES_LOADED})
    def "get patient using new data format new style query with value"(){
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"

        when: "I get all patients from the study that have concept Race with value Caucasian"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                        [type: ValueConstraint, valueType: STRING, operator: EQUALS, value:'Caucasian']
                ]
        ]

        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap)).patients

        then: "2 patients are returned"
        that responseData.size(), is(2)
        that responseData, everyItem(hasKey('id'))
        that responseData, everyItem(hasEntry('race', 'Caucasian'))
    }

}
