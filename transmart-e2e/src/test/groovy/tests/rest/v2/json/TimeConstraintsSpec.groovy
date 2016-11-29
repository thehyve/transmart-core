package tests.rest.v2.json

import base.RESTSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.*
import static tests.rest.v2.constraints.*

/**
 * TMPREQ-10
 *  The REST API should support querying observations based on a combination of:
 *      start time
 *      end time
 */
class TimeConstraintsSpec extends RESTSpec{

    /**
     *  given: "Ward-EHR is loaded"
     *  when: "I query observations in this study with startDate after 01-01-2016"
     *  then: "6 observations are returned"
     */
    @Requires({EHR_LOADED})
    def "query observations based on time constraint after startDate"(){
        given: "Ward-EHR is loaded"
        def date = toDateString("01-01-2016Z")

        when: "I query observations in this study with startDate after 01-01-2016"
        def constraintMap = [
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: StudyConstraint, studyId: EHR_ID],
                                [type: TimeConstraint,
                                 field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: 'DATE' ],
                                 operator: AFTER,
                                 values: [date]]
                        ]
                ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "6 observations are returned"
        that responseData.size(), is(6)
        that responseData, everyItem(hasKey('conceptCode'))
    }

    /**
     *  given: "Ward-EHR is loaded"
     *  when: "I query observations in this study with startDate between 29-3-2016 10:00:00 and 29-3-2016 10:11:00"
     *  then: "2 observations are returned"
     */
    @Requires({EHR_LOADED})
    def "query observations based on time constraint between startDates"(){
        given: "Ward-EHR is loaded"
        def date1 = toDateString("29-3-2016 10:00:00Z", "dd-MM-yyyy HH:mm:ssX")
        def date2 = toDateString("29-3-2016 10:11:00Z", "dd-MM-yyyy HH:mm:ssX")


        when: "I query observations in this study with startDate between 29-3-2016 10:00:00 and 29-3-2016 10:11:00"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyConstraint, studyId: EHR_ID],
                        [type: TimeConstraint,
                         field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: 'DATE' ],
                         operator: BETWEEN,
                         values: [date1, date2]]
                ]
        ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "2 observations are returned"
        that responseData.size(), is(2)
        that responseData, everyItem(hasKey('conceptCode'))
    }

    /**
     *  given: "Ward-EHR is loaded"
     *  when: "I query observations in this study with startDate before 01-01-2016"
     *  then: "1 observation is returned"
     */
    @Requires({EHR_LOADED})
    def "query observations based on time constraint before startDate"(){
        given: "Ward-EHR is loaded"
        def date = toDateString("01-01-2016Z")

        when: "I query observations in this study with startDate before 01-01-2016"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyConstraint, studyId: EHR_ID],
                        [type: TimeConstraint,
                         field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: 'DATE' ],
                         operator: BEFORE,
                         values: date]
                ]
        ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "1 observation is returned"
        that responseData.size(), is(6)
        that responseData, everyItem(hasKey('conceptCode'))
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I query observations in this study with startDate after 01-01-2016 and an endDate before 01-04-2016"
     *  then: "4 observations are returned"
     */
    @Requires({EHR_LOADED})
    def "query observations based on starDate and endDate"(){
        given: "EHR is loaded"
        def date1 = toDateString("01-01-2016Z")
        def date2 = toDateString("04-04-2016Z")

        when: "I query observations in this study with startDate after 01-01-2016 and an endDate before 01-04-2016"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyConstraint, studyId: EHR_ID],
                        [type: TimeConstraint,
                         field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: 'DATE' ],
                         operator: AFTER,
                         values: date1],
                        [type: TimeConstraint,
                         field: [dimension: 'EndTimeDimension', fieldName: 'endDate', type: 'DATE' ],
                         operator: BEFORE,
                         values: date2]
                ]
        ]

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "4 observations are returned"
        that responseData.size(), is(5)
        that responseData, everyItem(hasKey('conceptCode'))
    }
}