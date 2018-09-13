package org.transmartproject.core.userquery

import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User

/**
 * User query result changes resource
 */
interface UserQuerySetResource {

    /**
     * Scans for changes of results of the stored queries and updates stored sets
     */
    Integer scan()

    /**
     * Creates a new query set wit query set instances
     * @param query - related user query
     * @param currentUser user who saves the query.
     * @return
     */
    void createSetWithInstances(UserQueryRepresentation query, User currentUser)

    /**
     * Gets a list of changes of query results for the queries based on query id.
     *
     * @param queryId - id of the query
     * @param maxNumberOfSets - max number of returned sets
     * @return List of querySets with added or removed object ids
     */
    List<UserQuerySetChangesRepresentation> getQueryChangeHistory(Long queryId, User currentUser, Integer maxNumberOfSets)
            throws AccessDeniedException, NoSuchResourceException

    /**
     * Gets a list of changes of query results for the queries the user subscribed for.
     *
     * Returned list contains updates only from the last day or week,
     * depending on the query subscription frequency setting.
     *
     * @param frequency - daily or weekly
     * @param user - user whose queries to fetch
     * @param maxNumberOfSets - max number of returned sets
     * @return List of querySets with added or removed object ids
     */
    List<UserQuerySetChangesRepresentation> getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency frequency,
                                                                                        String username,
                                                                                        Integer maxNumberOfSets)

}
