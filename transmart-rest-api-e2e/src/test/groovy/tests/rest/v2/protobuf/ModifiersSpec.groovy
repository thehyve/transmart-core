package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector
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
        def constraintMap = [type: constraints.StudyNameConstraint, studyId: TUMOR_NORMAL_SAMPLES_ID]

        when: "I get all observations"
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))
        ObservationSelector selector = new ObservationSelector(responseData)

        then: "the modifiers are included"
        selector.cellCount == 19
        HashSet modifierDimension = []
        (0..<selector.cellCount).each {
            modifierDimension.add(selector.select(it, "sample_type", 'String'))
            assert selector.select(it, "study", "name", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension.size() == 3
        assert modifierDimension.containsAll(null, 'Tumor', 'Normal')
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included as a Dimension"
     */
    def "get modifiers by concept"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"


        def constraintMap = [
                type: Combination,
                operator: OR,
                args: [
                        [type: ConceptConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Lab\\Cell Count\\"],
                        [type: ConceptConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\HD\\Breast\\"]
                ]
        ]

        when: "I get all observations"
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))
        ObservationSelector selector = new ObservationSelector(responseData)

        then: "the modifiers are included"
        selector.cellCount == 10
        HashSet modifierDimension = []

        (0..<selector.cellCount).each {
            modifierDimension.add(selector.select(it, "sample_type", 'String'))
            assert selector.select(it, "study", "name", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension.size() == 2
        assert modifierDimension.containsAll('Tumor', 'Normal')
    }
}