package org.transmartproject.core.querytool

import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException

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
    QueryResult runQuery(QueryDefinition definition) throws InvalidRequestException

    /**
     * Fetches a {@link QueryResult} using its id.
     *
     * @param id the id of the query result to be fetched
     * @return the query result requested
     * @throws NoSuchResourceException if the there is no query result with
     * the given id
     */
    QueryResult getQueryResultFromId(Long id) throws NoSuchResourceException

    /**
     * Fetches the {@link QueryDefinition} used to obtain the passed in result.
     *
     * @param result the result whose generating definition is requested
     * @return the requested definition
     * @throws NoSuchResourceException if the definition cannot be found
     */
    QueryDefinition getQueryDefinitionForResult(QueryResult result) throws NoSuchResourceException

}
