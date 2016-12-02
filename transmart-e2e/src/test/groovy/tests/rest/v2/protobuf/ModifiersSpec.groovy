package tests.rest.v2.protobuf

import base.RESTSpec
import static config.Config.*
import protobuf.ObservationsMessageProto
import selectors.protobuf.ObservationSelector
import spock.lang.IgnoreIf
import tests.rest.v2.constraints

class ModifiersSpec extends RESTSpec {

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations"
     *  then "the modifiers are included"
     */
    @IgnoreIf({SUPPRESS_UNIMPLEMENTED})
    def "get modifiers"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def constraintMap = [type: constraints.StudyNameConstraint, studyId: TUMOR_NORMAL_SAMPLES_ID]

        when: "I get all observations"
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))
        ObservationSelector selector = new ObservationSelector(responseData)

        then: "the modifiers are included"
        HashSet modifierDimension = []
        (0..<selector.cellCount).each {
            modifierDimension.add(selector.select(it, "modifierDimension", "modifierCode", 'String'))
            assert selector.select(it, "modifierDimension", "studyId", 'String').equals(TUMOR_NORMAL_SAMPLES_ID)
        }
        assert modifierDimension.size() == 1
        assert modifierDimension.contains('TNS:SMPL')
    }


}
