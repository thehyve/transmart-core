/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static base.ContentTypeFor.contentTypeForProtobuf
import static config.Config.PATH_OBSERVATIONS
import static config.Config.TUMOR_NORMAL_SAMPLES_ID
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.ModifierConstraint
import static tests.rest.v2.constraints.ValueConstraint

/**
 *   TMPREQ-12 Support storing and fetching multiple samples.
 */
@RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
class SamplesSpec extends RESTSpec {

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "Sample type" with value "Tumor""
     *  then: "8 observations are returned, with concept Cell Count, Breast, Lung"
     */
    def "get observations related to a modifier"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ])
        ]

        when: "I get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "8 observations are returned, all have a cellcount"
        assert selector.cellCount == 8
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "concept", "conceptCode", 'String'))
            assert selector.select(it) != null
        }

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "Sample type" without value"
     *  then: "16 observations are returned, with concept Cell Count, Breast, Lung"
     */
    def "get observations related to a modifier without value"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type: ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\"
                ])
        ]

        when: "I get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "8 observations are returned, all have a cellcount"
        assert selector.cellCount == 16
        (0..<selector.cellCount).each {
            assert ['TNS:HD:EXPLUNG', 'TNS:HD:EXPBREAST', 'TNS:LAB:CELLCNT'].contains(selector.select(it, "concept", "conceptCode", 'String'))
            assert selector.select(it) != null
        }

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "does not exist"
     *  then: "0 observations are returned"
     */
    def "get observations related to a modifier that does not exist"() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type: ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\does not exist\\"
                ])
        ]

        when: "I get all observations related to a modifier 'does not exist'"
        def responseData = get(request)

        then: "0 observations are returned"
        assert responseData.cells == []

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}
