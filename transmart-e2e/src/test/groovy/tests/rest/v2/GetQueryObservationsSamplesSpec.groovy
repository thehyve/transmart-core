package tests.rest.v2

import base.RESTSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.SUPPRESS_KNOWN_BUGS
import static config.Config.TUMOR_NORMAL_SAMPLES_LOADED
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.ModifierConstraint
import static tests.rest.v2.constraints.ValueConstraint

class GetQueryObservationsSamplesSpec extends RESTSpec{

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "i get all observations related to a modifier "Sample type" with value "Tumor""
     *  then: "3 observations are returned, all have a cellcount"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //TMPDEV-97 ModifierConstraint returns modifier observations with null values
    def "get observations related to a modifier"(){
        given: "study TUMOR_NORMAL_SAMPLES is loaded"

        when: "i get all observations related to a modifier 'Sample type' with value 'Tumor'"
        def constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL", path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "3 observations are returned, all have a cellcount"
        that responseData.size(), is(3)
        that responseData, everyItem(allOf(
                hasEntry('modifierCd', 'TNS:SMPL'),
                hasEntry('conceptCode', 'TNS:LAB:CELLCNT'),
                not(hasEntry('numberValue', null))
        ))
    }
}
