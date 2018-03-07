/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.constraints.Negation
import static tests.rest.constraints.TrueConstraint

/**
 *  CRUD endpoint for user queries.
 */
class UserQuerySpec extends RESTSpec {

    def "list queries"() {
        createQuery()

        when:
        def getResponseData = getQueriesForUser()

        then:
        !getResponseData.queries.empty
    }

    def "get query"() {
        Long id = createQuery(DEFAULT_USER).id

        when:
        def responseData = getQuery(id)

        then:
        responseData.id == id

        when: 'trying to access the query by different user'
        def responseData2 = getQuery(id, 403, UNRESTRICTED_USER)

        then:
        responseData2.message == 'Query does not belong to the current user.'
    }

    def "save query"() {
        when:
        def responseData = createQuery()

        then:
        !('username' in responseData)
        !('deleted' in responseData)
        responseData.id != null
        responseData.name == 'test query'
        responseData.patientsQuery.type == 'true'
        responseData.observationsQuery.type == 'negation'
        responseData.apiVersion != null
        responseData.bookmarked == true
        responseData.subscribed == true
        responseData.subscriptionFreq == 'DAILY'

        responseData.createDate.endsWith('Z')
        responseData.updateDate.endsWith('Z')
    }

    def "save query wo patients and observations queries"() {
        when:
        def responseData = post([
                path      : PATH_QUERY,
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 400,
                body      : toJSON([
                        name             : 'test query',
                        patientsQuery    : null,
                        observationsQuery: null,
                        bookmarked       : true,
                        subscribed       : true,
                        subscriptionFreq : 'DAILY'
                ]),
        ])
        then:
        responseData.message == 'patientsQuery or observationsQuery has to be not null.'
    }

    def "update query"() {
        Long id = createQuery(DEFAULT_USER).id

        when:
        def updateResponseData = put([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 204,
                body      : toJSON([
                        name             : 'test query 2',
                        patientsQuery    : [type: Negation, arg: [type: TrueConstraint]],
                        observationsQuery: null,
                        bookmarked       : false,
                        subscribed       : false,
                        subscriptionFreq : 'WEEKLY'
                ]),
        ])

        then:
        updateResponseData == null
        def updatedQuery = getQuery(id)
        updatedQuery.name == 'test query 2'
        updatedQuery.patientsQuery.type == 'negation'
        updatedQuery.observationsQuery == null
        updatedQuery.bookmarked == false
        responseData.subscribed == false
        responseData.subscriptionFreq == 'WEEKLY'

        when: 'try to update query by a different user'
        def updateResponseData1 = put([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : UNRESTRICTED_USER,
                statusCode: 403,
                body      : toJSON([
                        bookmarked: true
                ]),
        ])

        then:
        updateResponseData1.message == 'Query does not belong to the current user.'
    }

    def "delete query"() {
        Long id = createQuery(DEFAULT_USER).id

        when:
        def forbidDeleteResponseData = delete([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : UNRESTRICTED_USER,
                statusCode: 403,
        ])
        then:
        forbidDeleteResponseData.message == 'Query does not belong to the current user.'

        when:
        def deleteResponseData = delete([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 204,
        ])

        then:
        deleteResponseData == null
        getQuery(id, 404).message == "Query with id ${id} has not found."
        !getQueriesForUser().queries.any { it.id == id }
    }

    def createQuery(String user = DEFAULT_USER) {
        post([
                path      : PATH_QUERY,
                acceptType: JSON,
                user      : user,
                statusCode: 201,
                body      : toJSON([
                        name             : 'test query',
                        patientsQuery    : [type: TrueConstraint],
                        observationsQuery: [type: Negation, arg: [type: TrueConstraint]],
                        bookmarked       : true,
                        subscribed       : true,
                        subscriptionFreq : 'DAILY'
                ]),
        ])
    }

    def getQuery(Long id, Integer statusCode = 200, String user = DEFAULT_USER) {
        get([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : user,
                statusCode: statusCode,
        ])
    }

    def getQueriesForUser() {
        get([
                path      : PATH_QUERY,
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 200,
        ])
    }
}
