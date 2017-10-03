package org.transmartproject.core.userquery

import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User

/**
 * User queries resource
 */
interface UserQueryResource {

    /**
     * @param currentUser user whose queries to fetch
     * @return list of all queries for the given currentUser.
     */
    List<UserQuery> list(User currentUser)

    /**
     * Gets query by it's identifier
     * @param id identifier of the query
     * @param currentUser user who fetches the query. It is expected to be the owner of the query.
     * Otherwise @see AccessDenedException is thrown.
     * @return a query or throws @see NoSuchResourceException if query with such id is not found.
     */
    UserQuery get(Long id, User currentUser) throws AccessDeniedException, NoSuchResourceException

    /**
     * Creates a new query bean
     * @param currentUser user for whom to create the query
     * @return
     */
    UserQuery create(User currentUser)

    /**
     * Persists a new or updates existing query
     * @param userQuery query to save
     * @param currentUser user who saves the query. It is expected to be the owner of the query.
     * Otherwise @see AccessDenedException is thrown.
     * @return currentUser query that is created with some fields updated by the system (e.g. createDate,...)
     * It throws @see InvalidArgumentsException if the query object is invalid.
     */
    void save(UserQuery userQuery, User currentUser) throws AccessDeniedException, InvalidArgumentsException

    /**
     * Deletes query with specified id
     * @param id identifier of the query
     * @param currentUser user who deletes the query. It is expected to be the owner of the query.
     * Otherwise @see AccessDenedException is thrown.
     */
    void delete(Long id, User currentUser) throws AccessDeniedException

}