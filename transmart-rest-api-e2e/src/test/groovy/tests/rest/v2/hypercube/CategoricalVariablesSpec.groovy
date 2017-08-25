/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.CATEGORICAL_VALUES_ID
import static config.Config.PATH_OBSERVATIONS
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
class CategoricalVariablesSpec extends RESTSpec {

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all observations from the study that have concept Gender"
     *  then: "no observations are returned"
     */
    def "get observations using old data format new style query"(def acceptType) {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\"])
        ]

        when: "I get all observations from the  study that have concept Gender"
        def responseData = get(request)

        then: "no observations are returned"
        assert responseData.cells == []

        where:
        acceptType | _
        JSON       | _
        PROTOBUF   | _
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
     *  when: "I get all observations from the study that have concept Gender\Female"
     *  then: "1 observation is returned"
     */
    def "get observations using old data format old style query"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the old data format"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Gender\\Female\\"])
        ]

        when: "I get all observations from the study that have concept Female"
        def responseData = get(request)

        then: "1 observation is returned"
        def selector = newSelector(responseData)

        assert selector.cellCount == 1
        assert selector.select(0, "concept", "conceptCode", 'String') == 'CV:DEM:SEX:F'
        assert selector.select(0) == 'Female'

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all observations from the study that have concept Race"
     *  then: "2 observations are returned"
     */
    def "get observations using new data format new style query"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"])
        ]

        when: "I get all observations from the study that have concept Race"
        def responseData = get(request)

        then: "3 observations are returned"
        def selector = newSelector(responseData)

        assert selector.cellCount == 3
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('CV:DEM:RACE')
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
     *  when: "I get all observations from the study that have concept Race with value Caucasian"
     *  then: "2 observations are returned"
     */
    def "get observations using new data format new style query with value"() {
        given: "study CATEGORICAL_VALUES is loaded where Gender is stored in the new data format"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\"],
                                [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: 'Caucasian']
                        ]
                ])
        ]

        when: "I get all observations from the study that have concept Race with value Caucasian"
        def responseData = get(request)

        then: "2 observations are returned"
        def selector = newSelector(responseData)

        assert selector.cellCount == 2
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('CV:DEM:RACE')
            assert selector.select(it) == 'Caucasian'
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }
}
