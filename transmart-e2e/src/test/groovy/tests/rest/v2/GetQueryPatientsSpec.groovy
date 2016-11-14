package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static config.Config.CLINICAL_TRIAL_LOADED
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.GREATER_THAN
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.constraints.*

/**
 *  TMPREQ-10
 *      The REST API should support querying patients based on observations:
 *          certain constraints are valid for any or for all observations for the patient. E.g, all observations of high blood pressure occur after supply of drug X.
 */
class GetQueryPatientsSpec extends RESTSpec{

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study with a heart rate above 80"
     *  then: "2 patients are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "get patients based on observations"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get all patients from that study with a heart rate above 80"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path:"\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:80]
                ]
        ]
        def responseData = get("query/patients", contentTypeForJSON, toQuery(constraintMap))

        then: "2 patients are returned"
        that responseData.size(), is(2)
        that responseData, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
     *  then: "2 patients are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "get patients by trial visit observation value"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path:"\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:60],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: GREATER_THAN,
                         value:7]
                ]
        ]
        def responseData = get("query/patients", contentTypeForJSON, toQuery(constraintMap))

        then: "2 patients are returned"
        that responseData.size(), is(2)
        that responseData, everyItem(hasKey('id'))
    }

}
