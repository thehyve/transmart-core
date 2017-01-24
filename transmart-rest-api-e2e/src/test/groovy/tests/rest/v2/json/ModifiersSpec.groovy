package tests.rest.v2.json

import base.RESTSpec
import tests.rest.v2.constraints

import static config.Config.PATH_OBSERVATIONS
import static config.Config.TUMOR_NORMAL_SAMPLES_ID
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.Combination
import static tests.rest.v2.constraints.ConceptConstraint

class ModifiersSpec extends RESTSpec {

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included as a Dimension"
     */
    def "get modifiers"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: constraints.StudyNameConstraint, studyId: TUMOR_NORMAL_SAMPLES_ID])
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
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included as a Dimension"
     */
    def "get modifiers by concept"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([
                        type: Combination,
                        operator: OR,
                        args: [
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
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}
