package org.transmartproject.core.userquery

import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User

/**
 * User query result changes resource
 */
interface UserQueryDiffResource {

    /**
     * Scans for changes of results of the stored queries and updates stored sets
     *
     * Allowed only for users with admin privileges
     *
     * @param currentUser
     * @throws AccessDeniedException
     */
    Integer scan(User currentUser) throws AccessDeniedException

    /**
     * Gets a list of changes of query results for the queries based on query id.
     *
     * @param queryId - id of the query
     * @param firstResult - parameter required to support pagination
     * @param numResults - parameter required to support pagination
     * @return List of queryDiffEntries
     */
    List<UserQueryDiffEntry> getAllEntriesByQueryId(Long queryId, User currentUser, int firstResult, Integer numResults)
            throws AccessDeniedException, NoSuchResourceException

    /**
     * Gets a list of changes of query results for the queries the user subscribed for.
     *
     * Returned list contains updates only from the last day or week,
     * depending on the query subscription frequency setting.
     *
     * @param frequency - daily or weekly
     * @param user - user whose queries to fetch
     * @param firstResult - parameter required to support pagination
     * @param numResults - parameter required to support pagination
     * @return List of queryDiffEntries
     */
    List<UserQueryDiffEntry> getAllEntriesByUsernameAndFrequency(String frequency, String username, int firstResult, Integer numResults)

}
