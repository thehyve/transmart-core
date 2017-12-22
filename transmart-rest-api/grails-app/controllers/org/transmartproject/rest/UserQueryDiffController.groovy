package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.userquery.UserQueryDiff
import org.transmartproject.core.userquery.UserQueryDiffResource
import org.transmartproject.rest.misc.CurrentUser

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class UserQueryDiffController {

    @Autowired
    CurrentUser currentUser

    @Autowired
    UserQueryDiffResource userQueryDiffResource

    static responseFormats = ['json']

    /**
     * Scans for changes in results of the stored queries and updates stored sets:
     * <code>/${apiVersion}/query_diffs/scan</code>
     *
     * This endpoint should be called after loading, deleting or updating the data.
     * Only available for administrators.
     */
    def scan() {
        userQueryDiffResource.scan(currentUser)
        response.status = 204
    }

    /**
     * Gets a list of query result change entries by query id - history of data changes for specific query:
     * <code>/${apiVersion}/query_diffs/${queryId}</code>
     *
     * @param firstResult - first result - parameter required for pagination, by default equals 0
     * @param numResults - number of results - parameter required for pagination
     * @param queryId - id of the query
     * @return list of queryDiffs
     */
    def getByQueryId(@PathVariable('queryId') Long queryId) {
        checkForUnsupportedParams(params, ['queryId', 'firstResult', 'numResults'])
        int firstResult = params.firstResult ?: 0
        def numResults = params.numResults as Integer

        List<UserQueryDiff> queryDiffs = userQueryDiffResource.getAllByQueryId(queryId, currentUser, firstResult, numResults)
        respond([queryDiffs: queryDiffs.collect { toResponseMap(it) }])
    }

    private static Map<String, Object> toResponseMap(UserQueryDiff queryDiff) {
        queryDiff.with {
            [
                    id               : id,
                    queryName        : queryName,
                    queryUsername    : queryUsername,
                    setId            : setId,
                    setType          : setType,
                    date             : date,
                    queryDiffEntries : queryDiffEntries
            ]
        }
    }
}
