/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.QueryResultWrapper

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class ObservationQueryController extends AbstractQueryController {

    @Autowired
    VersionController versionController

    static responseFormats = ['json', 'hal']

    /**
     * Finds a observation set query result:
     * <code>GET /v2/observation_sets/${id}</code>
     *
     * Finds the query result ({@link org.transmartproject.core.querytool.QueryResult}) with result instance id <code>id</code>.
     *
     * @return a map with the query result id, size and status.
     */
    def findObservationSetQueryResult(
            @PathVariable('id') Long id) {
        checkForUnsupportedParams(params, ['id'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        QueryResult queryResult = multiDimService.findQueryResult(id, user)

        render toQueryResultWrapper(queryResult) as JSON
    }

    /**
     * Gets all observation set queries for the current user.
     * <code>GET /v2/observation_sets</code>
     *
     * Finds all the observation set queries that User has access to.
     *
     * @return a list of maps with the query result id, size and status.
     */
    def findObservationSetQueryResultsForCurrentUser() {
        checkForUnsupportedParams(params, [])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Iterable<QueryResult> queryResults = multiDimService.findObservationSetQueryResults(user)

        respond toContainerResponseWrapper(queryResults)
    }

    /**
     * Observation set query creation endpoint:
     * <code>POST /v2/observation_sets?constraint=${constraint}&name=${name}</code>
     *
     * Creates a query result ({@link org.transmartproject.core.querytool.QueryResult}) based the {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map with the query result id, description, size, status, constraints and api version.
     */
    def createObservationSetQueryResult(
            @RequestParam('api_version') String apiVersion,
            @RequestParam('name') String name) {
        checkForUnsupportedParams(params, ['name', 'constraint'])
        if (name) {
            name = URLDecoder.decode(name, 'UTF-8').trim()
        } else {
            throw new InvalidArgumentsException("Parameter 'name' is missing.")
        }
        if (name.empty) {
            throw new InvalidArgumentsException("Empty 'name' parameter.")
        }

        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)
        if (mimeType != MimeType.JSON) {
            throw new InvalidRequestException("Content type should be " +
                    "${MimeType.JSON.name}; got ${mimeType}.")
        }

        def bodyJson = request.JSON
        log.debug "body JSON: $bodyJson"

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        String currentVersion = versionController.currentVersion(apiVersion)

        QueryResult queryResult = multiDimService
                .createObservationSetQueryResult(name, user, bodyJson.toString(), currentVersion)

        response.status = 201
        render toQueryResultWrapper(queryResult) as JSON
    }

    private static ContainerResponseWrapper toContainerResponseWrapper(Iterable<QueryResult> source) {
        new ContainerResponseWrapper(
                key: 'observationSets',
                container: source.collect {
                    toQueryResultWrapper(it)
                },
                componentType: QueryResult,
        )
    }

    private static QueryResultWrapper toQueryResultWrapper(QueryResult queryResult) {
        //FIXME interface leak
        if (queryResult instanceof QtQueryResultInstance) {
            new QueryResultWrapper(
                    apiVersion: queryResult.queryInstance.queryMaster.apiVersion,
                    queryResult: queryResult,
                    requestConstraint: queryResult.queryInstance.queryMaster.requestConstraints
            )
        } else {
            throw new IllegalStateException("Rendering of ${queryResult.class} class is not supported.")
        }

    }

}
