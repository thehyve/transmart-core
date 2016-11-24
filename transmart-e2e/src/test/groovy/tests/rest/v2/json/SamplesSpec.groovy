package tests.rest.v2.json

import base.RESTSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.ModifierConstraint
import static tests.rest.v2.constraints.ValueConstraint

class SamplesSpec extends RESTSpec{

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier "Sample type" with value "Tumor""
     *  then: "3 observations are returned, all have a cellcount"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
//    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //TMPDEV-97 ModifierConstraint returns modifier observations with null values
    def "get observations related to a modifier"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "I get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL", path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "3 observations are returned, all have a cellcount"
        that responseData.size(), is(3)
        that responseData, everyItem(allOf(
                hasEntry('conceptCode', 'TNS:LAB:CELLCNT'),
                not(hasEntry('numberValue', null))
        ))
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get all observations related to a modifier 'Sample ID' with value 'id'"
     *  then: "3 observations are returned with concept codes: CELLCNT, .., ..."
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    @IgnoreIf({SUPPRESS_KNOWN_BUGS || SUPPRESS_UNIMPLEMENTED}) //TMPDEV-97 ModifierConstraint returns modifier observations with null values. no test set with multiple concepts per sample.
    def "get observations related to a"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "I get all observations related to a modifier 'Sample ID' with value 'id'"
        def constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL", path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample ID\\",
                values: [type: ValueConstraint, valueType: NUMERIC, operator: EQUALS, value: 10]
        ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "3 observations are returned, all have a cellcount"
        that responseData.size(), is(3)
        that responseData, allOf(
                hasItem(
                        hasEntry('conceptCode', 'TNS:LAB:CELLCNT'),
                        not(hasEntry('numberValue', null))
                ),
                hasItem(
                        hasEntry('conceptCode', '...'),
                        not(hasEntry('numberValue', null))
                ),
                hasItem(
                        hasEntry('conceptCode', '...'),
                        not(hasEntry('numberValue', null))
                )
        )
    }
}
