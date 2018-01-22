package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.userquery.UserQuerySetDiff
import org.transmartproject.core.userquery.UserQuerySetInstance
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.rest.misc.CurrentUser

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class UserQuerySetController {

    @Autowired
    CurrentUser currentUser

    @Autowired
    UserQuerySetResource userQuerySetResource

    static responseFormats = ['json']

    /**
     * Scans for changes in results of the stored queries and updates stored sets:
     * <code>/${apiVersion}/query_sets/scan</code>
     *
     * This endpoint should be called after loading, deleting or updating the data.
     * Only available for administrators.
     *
     * @return number of sets that were updated, which is also a number of created querySets
     */
    def scan() {
        Integer result = userQuerySetResource.scan(currentUser)
        response.status = 201
        respond([numberOfUpdatedSets: result])
    }

    /**
     * Gets a list of query sets related to a specific query:
     * <code>/${apiVersion}/query_sets/${queryId}</code>
     *
     * @param firstResult - first result - parameter required for pagination, by default equals 0
     * @param numResults - number of results - parameter required for pagination
     * @param queryId - id of the query
     * @return list of querySets
     */
    def getSetsByQueryId(@PathVariable('queryId') Long queryId) {
        checkForUnsupportedParams(params, ['queryId', 'firstResult', 'numResults'])
        int firstResult = params.firstResult ?: 0
        def numResults = params.numResults as Integer

        List<UserQuerySetInstance> querySetInstances = userQuerySetResource.getSetInstancesByQueryId(queryId, currentUser,
                firstResult, numResults)
        def querySetInstancesMap = querySetInstances?.groupBy { it.querySet }
        respond([querySetInstances: querySetInstancesMap ?
                querySetInstancesMap.collect { toResponseMap(it, UserQuerySetInstance.class) } : []])
    }

    /**
     * Gets a list of query result change entries by query id - history of data sets changes for specific query:
     * <code>/${apiVersion}/query_sets/diffs/${queryId}</code>
     *
     * @param firstResult - first result - parameter required for pagination, by default equals 0
     * @param numResults - number of results - parameter required for pagination
     * @param queryId - id of the query
     * @return list of queryDiffs
     */
    def getDiffsByQueryId(@PathVariable('queryId') Long queryId) {
        checkForUnsupportedParams(params, ['queryId', 'firstResult', 'numResults'])
        int firstResult = params.firstResult ?: 0
        def numResults = params.numResults as Integer

        List<UserQuerySetDiff> setDiffs = userQuerySetResource.getDiffEntriesByQueryId(queryId, currentUser,
                firstResult, numResults)
        def queryDiffsMap = setDiffs?.groupBy { it.querySet }
        respond([querySetDiffs: queryDiffsMap ? queryDiffsMap.collect { toResponseMap(it, UserQuerySetDiff.class) } : []])
    }

    private static Map<String, Object> toResponseMap(response, Class<?> classType) {
        response.with {
            def responseMap = [
                    id               : it.key.id,
                    queryName        : it.key.query.name,
                    queryUsername    : it.key.query.username,
                    setSize          : it.key.setSize,
                    setType          : it.key.setType,
                    createDate       : it.key.createDate,
            ]
            if (classType == UserQuerySetDiff.class){
                responseMap.put('diffs', value ? value.collect { entry ->
                    [
                            "id"        : entry.id,
                            "objectId"  : entry.objectId,
                            "changeFlag": entry.changeFlag
                    ]
                } : [])
            } else {
                responseMap.put('instances', value ? value.collect { entry ->
                    [
                            "id"        : entry.id,
                            "objectId"  : entry.objectId
                    ]
                } : [])
            }
            responseMap
        }
    }
}
