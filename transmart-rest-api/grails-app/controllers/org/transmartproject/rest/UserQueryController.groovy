package org.transmartproject.rest

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.querytool.Query
import org.transmartproject.rest.misc.CurrentUser

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

//TODO Stop using core-db directly
class UserQueryController {

    @Autowired
    VersionController versionController

    @Autowired
    CurrentUser currentUser

    private static final JsonSlurper JSON_SLURPER = new JsonSlurper()

    static responseFormats = ['json']

    def index() {
        List<Query> queries = Query.findAllByUsernameAndDeleted(currentUser.username, false)
        respond([queries: queries.collect { toResponseMap(it) }])
    }

    def get(@PathVariable('id') Long id) {
        checkForUnsupportedParams(params, ['id'])
        Query query = Query.findByIdAndDeleted(id, false)
        if (query) {
            if (currentUser.username != query.username) {
                throw new AccessDeniedException("Query does not belong to the current user.")
            }
            respond toResponseMap(query)
        } else {
            throw new NoSuchResourceException("Query with id ${id} has not found.")
        }
    }

    def save(@RequestParam('api_version') String apiVersion) {
        def requestJson = request.JSON
        checkForUnsupportedParams(requestJson, ['name', 'patientsQuery', 'observationsQuery', 'bookmarked'])
        if (!requestJson.patientsQuery && !requestJson.observationsQuery) {
            throw new InvalidArgumentsException("At least one parameter of two has to be provided: patientsQuery, observationsQuery")
        }

        //TODO Perhaps reuse existing logic
        if (requestJson.patientsQuery && !requestJson.patientsQuery.type) {
            throw new InvalidArgumentsException("patientsQuery does not contain valid constraint.")
        }
        if (requestJson.observationsQuery && !requestJson.observationsQuery.type) {
            throw new InvalidArgumentsException("observationsQuery does not contain valid constraint.")
        }
        def now = new Date()
        def query = new Query(
                name: requestJson.name,
                username: currentUser.username,
                patientsQuery: requestJson.patientsQuery?.toString(),
                observationsQuery: requestJson.observationsQuery?.toString(),
                apiVersion: versionController.currentVersion(apiVersion),
                bookmarked: requestJson.bookmarked ?: false,
                deleted: false,
                createDate: now,
                updateDate: now,
        )
        if (query.validate()) {
            query.save(flush: true)
            response.status = 201
            respond toResponseMap(query)
        } else {
            throw new InvalidArgumentsException('Provided field are invalid.')
        }
    }

    def update(@RequestParam('api_version') String apiVersion,
               @PathVariable('id') Long id) {
        def requestJson = request.JSON
        checkForUnsupportedParams(requestJson, ['name', 'patientsQuery', 'observationsQuery', 'bookmarked'])

        //TODO Perhaps reuse existing logic
        if (requestJson.patientsQuery && !requestJson.patientsQuery.type) {
            throw new InvalidArgumentsException("patientsQuery does not contain valid constraint.")
        }
        if (requestJson.observationsQuery && !requestJson.observationsQuery.type) {
            throw new InvalidArgumentsException("observationsQuery does not contain valid constraint.")
        }
        Query query = Query.findByIdAndDeleted(id, false)
        if (!query) {
            throw new NoSuchResourceException("Query with id ${id} has not found.")
        }
        if (currentUser.username != query.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        if (requestJson.containsKey('name')) {
            query.name = requestJson.name
        }
        if (requestJson.containsKey('patientsQuery')) {
            query.patientsQuery = requestJson.patientsQuery?.toString()
        }
        if (requestJson.containsKey('observationsQuery')) {
            query.observationsQuery = requestJson.observationsQuery?.toString()
        }
        if (requestJson.containsKey('patientsQuery') || requestJson.containsKey('observationsQuery')) {
            query.apiVersion = versionController.currentVersion(apiVersion)
        }
        if (requestJson.containsKey('bookmarked')) {
            query.bookmarked = requestJson.bookmarked
        }
        query.updateDate = new Date()
        if (query.validate()) {
            response.status = 204
        } else {
            throw new InvalidArgumentsException('Provided field are invalid.')
        }
    }

    def delete(@PathVariable('id') Long id) {
        Query query = Query.findByIdAndDeleted(id, false)
        if (!query) {
            throw new NoSuchResourceException("Query with id ${id} has not found.")
        }
        if (currentUser.username != query.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        query.deleted = true
        query.updateDate = new Date()
        query.save()
        response.status = 204
    }

    private static Map<String, Object> toResponseMap(Query query) {
        query.with {
            [
                    id               : id,
                    name             : name,
                    patientsQuery    : patientsQuery ? JSON_SLURPER.parseText(patientsQuery) : null,
                    observationsQuery: observationsQuery ? JSON_SLURPER.parseText(observationsQuery) : null,
                    apiVersion       : apiVersion,
                    bookmarked       : bookmarked,
                    createDate       : createDate,
                    updateDate       : updateDate,
            ]
        }
    }
}
