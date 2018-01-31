package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.userquery.UserQuerySet
import org.transmartproject.core.userquery.UserQuerySetDiff
import org.transmartproject.core.userquery.UserQuerySetInstance
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.UserQuerySetDiffWrapper
import org.transmartproject.rest.marshallers.UserQuerySetInstanceWrapper
import org.transmartproject.rest.marshallers.UserQuerySetWrapper
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
        respond wrapQuerySetsWithInstances(querySetInstancesMap.keySet())
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
        respond wrapQuerySetsWithDiffs(queryDiffsMap.keySet())
    }

    private static wrapQuerySetsWithDiffs(Collection<UserQuerySet> querySets) {
        new ContainerResponseWrapper(
                key: 'querySetDiffs',
                container: querySets.collect { new UserQuerySetWrapper(
                        id               : it.id,
                        queryId          : it.query.id,
                        queryName        : it.query.name,
                        queryUsername    : it.query.username,
                        setSize          : it.setSize,
                        setType          : it.setType,
                        createDate       : it.createDate,
                        diffs            : it.querySetDiffs.collect{ diff ->
                                                new UserQuerySetDiffWrapper(
                                                        id        : diff.id,
                                                        objectId  : diff.objectId,
                                                        changeFlag: diff.changeFlag
                                                )
                                            }
                )},
                componentType: UserQuerySet
        )
    }

    private static wrapQuerySetsWithInstances(Collection<UserQuerySet> querySets) {
        new ContainerResponseWrapper(
                key: 'querySetInstances',
                container: querySets.collect { new UserQuerySetWrapper(
                        id               : it.id,
                        queryId          : it.query.id,
                        queryName        : it.query.name,
                        queryUsername    : it.query.username,
                        setSize          : it.setSize,
                        setType          : it.setType,
                        createDate       : it.createDate,
                        instances        : it.querySetDiffs.collect{ diff ->
                                                new UserQuerySetInstanceWrapper(
                                                        id      : diff.id,
                                                        objectId: diff.objectId
                                                )
                        }
                )},
                componentType: UserQuerySet
        )
    }

}
