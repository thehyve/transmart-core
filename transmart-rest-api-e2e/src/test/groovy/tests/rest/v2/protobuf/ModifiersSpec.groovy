package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector
import tests.rest.v2.constraints

import static config.Config.PATH_OBSERVATIONS
import static config.Config.TUMOR_NORMAL_SAMPLES_ID

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
            assert selector.select(it, "StudyDimension", "studyId", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension.size() == 3
        assert modifierDimension.containsAll('', 'Tumor', 'Normal')
    }
}
