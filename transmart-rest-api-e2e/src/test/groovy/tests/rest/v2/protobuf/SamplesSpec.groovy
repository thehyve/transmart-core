package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.ModifierConstraint
import static tests.rest.v2.constraints.ValueConstraint

/**
 *   TMPREQ-12 Support storing and fetching multiple samples.
 */
class SamplesSpec extends RESTSpec{

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "Sample type" with value "Tumor""
     *  then: "8 observations are returned, with concept Cell Count, Breast, Lung"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    def "get observations related to a modifier"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "I get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def constraintMap = [
                type: ModifierConstraint, path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "8 observations are returned, all have a cellcount"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 8
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "ConceptDimension", "conceptCode", 'String'))
            assert selector.select(it) != null
        }
    }


    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "Sample type" without value"
     *  then: "16 observations are returned, with concept Cell Count, Breast, Lung"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    def "get observations related to a modifier without value"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "I get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def constraintMap = [
                type: ModifierConstraint, path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\"
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "8 observations are returned, all have a cellcount"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 16
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "ConceptDimension", "conceptCode", 'String'))
            assert selector.select(it) != null
        }
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "does not exist"
     *  then: "0 observations are returned"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    def "get observations related to a modifier that does not exist"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "I get all observations related to a modifier 'does not exist'"
        def constraintMap = [
                type: ModifierConstraint, path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\does not exist\\"
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "0 observations are returned"
        assert responseData.header == null
    }
}
