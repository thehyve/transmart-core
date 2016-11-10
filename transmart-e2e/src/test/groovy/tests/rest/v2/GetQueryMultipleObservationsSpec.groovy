package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static config.Config.WARD_CLINICALTRAIL_LOADED
import static config.Config.WARD_EHR_LOADED
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

/**
 * TMPREQ-9
 */
class GetQueryMultipleObservationsSpec extends RESTSpec{

    def REGEXDATE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[A-Z]{1,3}"

    /**
     *  Given: "Ward-ClinicalTrial is loaded"
     *  When: "I get observations for patient 1 for concept HEARTRATE"
     *  Then: "3 observations are returned"
     */
    @Requires({WARD_CLINICALTRAIL_LOADED})
    def "Multiple observations are possible per combination of concept and patient"(){
        given: "Ward-ClinicalTrial is loaded"
        def id = 1

        when: "I get observations for patient 1 for concept HEARTRATE"
        def constraintMap = [
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: PatientSetConstraint, patientSetId: 0, patientIds: [id]],
                                [type: ConceptConstraint, path:"\'\\\\Public Studies\\\\Ward-ClinicalTrial\\\\Vitals\\\\HEARTRATE\\\\\'"]
                        ]
                ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "3 observations are returned"
        that responseData.size(), is(3)
    }

    /**
     *  given: "Ward-EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "7 observations have a startDate, all formated with a datestring"
     */
    @Requires({WARD_EHR_LOADED})
    def "Start time of observations are exposed through REST API"(){
        given: "Ward-EHR is loaded"
        def studyId = "Ward-EHR"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyConstraint, studyId: studyId]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "7 observations have a startDate, all formated with a datestring"
        def filteredResponseData = responseData.findAll {it.'startDate' != null}
        that filteredResponseData.size(), is(7)
        that filteredResponseData, everyItem(hasEntry(is('startDate'), matchesPattern(REGEXDATE)))
    }

    /**
     *  given: "Ward-EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "4 observations have a endDate, all formated with a datestring"
     */
    @Requires({WARD_EHR_LOADED})
    def "end time of observations are exposed through REST API"(){
        given: "Ward-EHR is loaded"
        def studyId = "Ward-EHR"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyConstraint, studyId: studyId]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "4 observations have a endDate, all formated with a datestring"
        def filteredResponseData = responseData.findAll {it.'endDate' != null}
        that filteredResponseData.size(), is(4)
        that filteredResponseData, everyItem(hasEntry(is('endDate'), matchesPattern(REGEXDATE)))
    }
}
