package org.transmartproject.rest.marshallers

import org.transmartproject.core.querytool.QueryResult

class QueryResultWrapper {
    String apiVersion
    QueryResult queryResult
    boolean embedPatients = false
}
