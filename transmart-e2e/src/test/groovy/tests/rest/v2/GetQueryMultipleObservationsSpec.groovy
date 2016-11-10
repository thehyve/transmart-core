package tests.rest.v2

import base.RESTSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.CLINICAL_TRIAL_LOADED
import static config.Config.EHR_ID
import static config.Config.EHR_LOADED
import static config.Config.REGEXDATE
import static config.Config.SUPPRESS_KNOWN_BUGS
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

/**
 *  TMPREQ-9
 *      Multiple observations
 */
class GetQueryMultipleObservationsSpec extends RESTSpec{

    /**
     *  Given: "ClinicalTrial is loaded"
     *  When: "I get observations for patient 1 for concept HEARTRATE"
     *  Then: "3 observations are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "Multiple observations are possible per combination of concept and patient"(){
        given: "Ward-ClinicalTrial is loaded"
        def id = 1

        when: "I get observations for patient 1 for concept HEARTRATE"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientSetId: 0, patientIds: -62],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "3 observations are returned"
        that responseData.size(), is(3)
        that responseData, everyItem(hasKey('conceptCode'))
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "7 observations have a valid startDate, all formated with a datestring"
     */
    @Requires({EHR_LOADED})
    def "Start time of observations are exposed through REST API"(){
        given: "EHR is loaded"
        def studyId = "EHR"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyConstraint, studyId: studyId]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "7 observations have a startDate, all formated with a datestring"
        def filteredResponseData = responseData.findAll {it.'startDate' != '1970-01-01T00:00:00Z'}
        that filteredResponseData.size(), is(7)
        that responseData, everyItem(hasKey('conceptCode'))
        that filteredResponseData, everyItem(hasEntry(is('startDate'), matchesPattern(REGEXDATE)))
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "4 observations have a nonNUll endDate, all formated with a datestring"
     */
    @Requires({EHR_LOADED})
    def "end time of observations are exposed through REST API"(){
        given: "EHR is loaded"
        def studyId = "EHR"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyConstraint, studyId: studyId]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "4 observations have a endDate, all formated with a datestring"
        def filteredResponseData = responseData.findAll {it.'endDate' != null}
        that filteredResponseData.size(), is(4)
        that responseData, everyItem(hasKey('conceptCode'))
        that filteredResponseData, everyItem(hasEntry(is('endDate'), matchesPattern(REGEXDATE)))
    }
}
