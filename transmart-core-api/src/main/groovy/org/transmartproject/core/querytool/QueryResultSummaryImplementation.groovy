package org.transmartproject.core.querytool

import groovy.transform.CompileStatic


/**
 * A query result instance contains information about a specific run of a
 * query.

 */
@CompileStatic
class QueryResultSummaryImplementation implements QueryResultSummary {

    QueryResult queryResult

    QueryResultSummaryImplementation(QueryResult queryResult){
        this.queryResult = queryResult
    }

    @Override
    Long getId() {
        queryResult.id
    }

    @Override
    Long getSetSize() {
        queryResult.setSize
    }

    @Override
    QueryStatus getStatus() {
        queryResult.status
    }

    @Override
    String getErrorMessage() {
        queryResult.errorMessage
    }

    @Override
    String getUsername() {
        queryResult.username
    }
}
