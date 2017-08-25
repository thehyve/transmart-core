/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec
import tests.rest.constraints

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.PATH_OBSERVATIONS
import static config.Config.TUMOR_NORMAL_SAMPLES_ID
import static constraints.Combination
import static constraints.ConceptConstraint
import static tests.rest.Operator.OR

@RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
class ModifiersSpec extends RESTSpec {

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included as a Dimension"
     */
    def "get modifiers"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: constraints.StudyNameConstraint, studyId: TUMOR_NORMAL_SAMPLES_ID])
        ]

        when: "I get all observations"
        def responseData = get(request)

        then: "the modifiers are included"
        def selector = newSelector(responseData)

        selector.cellCount == 19
        HashSet modifierDimension = []
        (0..<selector.cellCount).each {
            modifierDimension.add(selector.select(it, "sample_type", 'String'))
            assert selector.select(it, "study", "name", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension == [null, 'Tumor', 'Normal'] as HashSet

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included as a Dimension"
     */
    def "get modifiers by concept"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Lab\\Cell Count\\"],
                                [type: ConceptConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\HD\\Breast\\"]
                        ]
                ])
        ]

        when: "I get all observations"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "the modifiers are included"
        selector.cellCount == 10
        HashSet modifierDimension = []

        (0..<selector.cellCount).each {
            modifierDimension.add(selector.select(it, "sample_type", 'String'))
            assert selector.select(it, "study", "name", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension.size() == 2
        assert modifierDimension.containsAll('Tumor', 'Normal')

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }
}
