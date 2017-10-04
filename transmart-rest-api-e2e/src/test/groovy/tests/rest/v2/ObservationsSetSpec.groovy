/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import base.RESTSpec

import static config.Config.PATH_OBSERVATION_SET

import static base.ContentTypeFor.JSON
import static tests.rest.constraints.TrueConstraint

class ObservationsSetSpec extends RESTSpec {

    /**
     *  when: "I make an observation set."
     *  then: "I get a successful response."
     */
    def "create observation set"() {
        def request = [
                path      : PATH_OBSERVATION_SET,
                acceptType: JSON,
                query     : [name: 'test_observation_set'],
                body      : toJSON([type: TrueConstraint]),
                statusCode: 201
        ]

        when: "I make an observation."
        def responseData = post(request)

        then: "I get a successful reply."
        responseData.id != null
        responseData.requestConstraints.contains(TrueConstraint)
        responseData.description == 'test_observation_set'

    }

    /**
     *  when: "I ask an observation set query result."
     *  then: "I get get the observation set query result."
     */
    def "get saved observation set"() {
        def request = [
                path      : PATH_OBSERVATION_SET,
                acceptType: JSON,
                query     : [name: 'test_observation_set'],
                body      : toJSON([type: TrueConstraint]),
                statusCode: 201
        ]
        def responseData1 = post(request)

        when: "I ask an observation set query result."
        def responseData2 = get([
                path      : PATH_OBSERVATION_SET + '/' + responseData1.id,
                acceptType: JSON,
        ])

        then: "I get get the observation set query result."
        responseData1 == responseData2

    }

    /**
     *  when: "I ask observation set query results."
     *  then: "I get a list of observation set query results."
     */
    def "get saved observation sets"() {
        def request = [
                path      : PATH_OBSERVATION_SET,
                acceptType: JSON,
                query     : [name: 'test_observation_set'],
                body      : toJSON([type: TrueConstraint]),
                statusCode: 201
        ]
        def responseData1 = post(request)

        when: "I ask observation set query results."
        def responseData2 = get([
                path      : PATH_OBSERVATION_SET,
                acceptType: JSON,
        ])

        then: "I get a list of observation set query results."
        responseData1 in responseData2.observationSets

    }
}
