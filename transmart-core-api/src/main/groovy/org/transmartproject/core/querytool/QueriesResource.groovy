package org.transmartproject.core.querytool

import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User

/**
 *
 */
interface QueriesResource {

    /**
     * Creates and executes a query in one go. The query is run synchronously.
     *
     * @deprecated Use {@link #runQuery(QueryDefinition, User)} instead.
     * Query definitions have to be associated with the user that issue the queries
     * so that access control to the results can be enforced.
     *
     * @param definition the definition to use
     * @return the resulting query result
     */
    @Deprecated
    QueryResult runQuery(QueryDefinition definition) throws InvalidRequestException

    /**
     * Creates and executes a query in one go. The query is run synchronously.
     *
     * @param definition the definition to use
     * @param the user that issued the query.
     * @return the resulting query result
     */
    QueryResult runQuery(QueryDefinition definition, User user) throws InvalidRequestException

    /**
     * Creates and executes a query in one go to update queries resource data. The query is run synchronously.
     *
     * @param id ID of the cohort that is getting disabled.
     * @param user the user that issued the query.
     * @return the resulting query result
     */
    QueryResult disableQuery(Long id, User user) throws InvalidRequestException

    /**
     * Fetches a {@link QueryResult} using its id.
     *
     * @param id the id of the query result to be fetched
     * @param user the user that issued the query.
     * @return the query result requested
     * @throws NoSuchResourceException if there is no query result with
     * the given id or the user does not have access to the query.
     */
    QueryResult getQueryResultFromId(Long id, User user) throws NoSuchResourceException

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
    List<QueryResultSummary> getQueryResults(User user)

}
