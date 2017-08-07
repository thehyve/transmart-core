package org.transmartproject.rest.marshallers

import org.transmartproject.core.querytool.QueryResult

class QueryResultWrapper {
    String apiVersion
    QueryResult queryResult
    String requestConstraint
    boolean embedPatients = false
}
