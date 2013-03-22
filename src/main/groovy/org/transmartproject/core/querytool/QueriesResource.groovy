package org.transmartproject.core.querytool

/**
 *
 */
interface QueriesResource {

    /**
     * Creates and executes a query in one go. The query is run synchronously.
     *
     * @param definition the definition to use
     * @return the resulting query result
     */
    QueryResult runQuery(QueryDefinition definition)

}
