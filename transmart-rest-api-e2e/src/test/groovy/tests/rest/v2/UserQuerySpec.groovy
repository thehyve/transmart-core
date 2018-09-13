/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import base.RESTSpec
import base.RestHelper
import org.transmartproject.core.multidimquery.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.constraints.Negation
import static tests.rest.constraints.TrueConstraint
import static tests.rest.dimensions.*

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
        def responseData2 = getQuery(id, 404, UNRESTRICTED_USER)

        then:
        responseData2.message == "Query with id ${id} not found for user."
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
        responseData.queryBlob.dataTableState.rowDimensions == [Patient, Study]
        responseData.queryBlob.dataTableState.columnDimensions == [TrialVisit, Concept]
        responseData.queryBlob.dataTableState.sorting.dimensionName == Study
        responseData.queryBlob.dataTableState.sorting.order == "asc"

        responseData.createDate.endsWith('Z')
        responseData.updateDate.endsWith('Z')
    }

    def "save query wo patients and observations queries"() {
        when:
        def responseData = RestHelper.toObject post([
                path      : PATH_QUERY,
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 400,
                body      : [
                        name             : 'test query',
                        patientsQuery    : null,
                        observationsQuery: null,
                        bookmarked       : true,
                        subscribed       : true,
                        subscriptionFreq : 'DAILY'
                ],
        ]), ErrorResponse

        then:
        responseData.message == 'Cannot subscribe to an empty query.'
    }

    def "update query"() {
        Long id = createQuery(DEFAULT_USER).id

        when:
        def updatedQuery = put([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 200,
                body      : [
                        name             : 'test query 2',
                        bookmarked       : false,
                        subscribed       : false,
                        subscriptionFreq : 'WEEKLY'
                ],
        ])

        then:
        updatedQuery.name == 'test query 2'
        updatedQuery.bookmarked == false
        updatedQuery.subscribed == false
        updatedQuery.subscriptionFreq == 'WEEKLY'

        when: 'try to update query by a different user'
        def updateResponseData1 = RestHelper.toObject put([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : UNRESTRICTED_USER,
                statusCode: 404,
                body      : [
                        bookmarked: true
                ],
        ]), ErrorResponse

        then:
        updateResponseData1.message == "Query with id ${id} not found for user."
    }

    def "delete query"() {
        Long id = createQuery(DEFAULT_USER).id

        when:
        def forbidDeleteResponseData = delete([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : UNRESTRICTED_USER,
                statusCode: 404,
        ])
        then:
        forbidDeleteResponseData.message == "Query with id ${id} not found for user."

        when:
        def deleteResponseData = delete([
                path      : "${PATH_QUERY}/${id}",
                acceptType: JSON,
                user      : DEFAULT_USER,
                statusCode: 204,
        ])

        then:
        deleteResponseData == null
        getQuery(id, 404).message == "Query with id ${id} not found for user."
        !getQueriesForUser().queries.any { it.id == id }
    }

    def createQuery(String user = DEFAULT_USER) {
        post([
                path      : PATH_QUERY,
                acceptType: JSON,
                user      : user,
                statusCode: 201,
                body      : [
                        name             : 'test query',
                        patientsQuery    : [type: TrueConstraint],
                        observationsQuery: [type: Negation, arg: [type: TrueConstraint]],
                        bookmarked       : true,
                        subscribed       : true,
                        subscriptionFreq : 'DAILY',
                        queryBlob        : [
                                dataTableState: [
                                        rowDimensions   : [Patient, Study],
                                        columnDimensions: [TrialVisit, Concept],
                                        sorting         : [
                                                dimensionName: Study,
                                                order        : 'asc'
                                        ]
                                ]
                        ]
                ],
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
