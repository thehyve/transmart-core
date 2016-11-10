package tests.rest.v2

import base.RESTSpec
import spock.lang.Ignore

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AFTER
import static tests.rest.v2.constraints.*

/**
 * TMPREQ-11 Extend the REST API with timeseries data capabilities.
 */
class GetQueryObservationsTimeConstraintsSpec extends RESTSpec{

    def REGEXDATE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"

    /**
     *  when: 'I query all observations in the database'
     *  then: "Every observation has an entry with key 'startDate'"
     */
    def "observations must have a start time"(){
        when: 'I query all observations in the database'
        def query = [
                constraint: [
                        type: TrueConstraint,
                ]
        ]
        def responseData = get("query/observations", contentTypeForJSON, query)


        then: "Every observation has an entry with key 'startDate' that is not null"
        that responseData, everyItem(hasEntry(is('startDate'), matchesPattern(REGEXDATE)))
    }

    /**
     *  when: 'I query all observations in the database'
     *  then: "Every observation has an entry with key 'endDate' that is a valid date sting or null"
     */
    def "observations must have a end time"(){
        when: 'I query all observations in the database'
        def query = [
                constraint: [
                        type: TrueConstraint,
                ]
        ]
        def responseData = get("query/observations", contentTypeForJSON, query)

        then: "Every observation has an entry with key 'endDate' that is a valid date sting or null"
        that responseData, everyItem(hasEntry(is('endDate'), anyOf(nullValue(), matchesPattern(REGEXDATE))))
    }

    @Ignore
    def "Get /query/observations by startDate"(){
        when:
        def query = [
                constraint: [
                        type: TimeConstraint,
                        field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: 'DATE' ],
                        operator: AFTER,
                        values: ['2016-04-19']
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, query)


        then:
        that responseData.size(), is(952)
    }

    @Ignore
    def "Get /query/observations by "(){
        when:
        def query = [
                constraint: [
                        type: NullConstraint,
                        field: [dimension: 'StartTimeDimension', fieldName: 'endDate', type: 'DATE' ]
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, query)


        then:
        that responseData.size(), is(952)
    }

}
