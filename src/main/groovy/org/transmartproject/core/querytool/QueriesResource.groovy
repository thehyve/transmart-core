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
     * In a tranSMART environment, consider using
     * {@link #runQuery(QueryDefinition, String)} instead. Query
     * definitions have to be associated with the user that issue the queries
     * so that access control to the results can be implemented
     *
     * @param definition the definition to use
     * @return the resulting query result
     */
    QueryResult runQuery(QueryDefinition definition) throws InvalidRequestException

    /**
     * Creates and executes a query in one go. The query is run synchronously.
     *
     * @param definition the definition to use
     * @param username the user that issued the query. This is the username of
     * a tranSMART user (for usages in tranSMART) or an i2b2 user, for
     * compatibility with i2b2.
     * @return the resulting query result
     */
    QueryResult runQuery(QueryDefinition definition, String username) throws InvalidRequestException

    /**
     * Creates and executes a query in one go to update queries resource data. The query is run synchronously.
     *
     * @param definition the definition to use
     * @param username the user that issued the query. This is the username of
     * a tranSMART user (for usages in tranSMART) or an i2b2 user, for
     * compatibility with i2b2.
     * @return the resulting query result
     */
    QueryResult runDisablingQuery(Long id, String username) throws InvalidRequestException

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

    /**
     * Fetches a list of {@link QueryResult} by username.
     *
     * @param username of the creator of queries from which QueryResult originated
     * @return List of all QueryResults created by given user
     */
    List<QueryResult> getQueryResultsByUsername(String username)

}
