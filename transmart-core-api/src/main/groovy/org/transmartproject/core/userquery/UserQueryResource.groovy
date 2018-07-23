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
     * Retrieves all queries owned by the current user.
     *
     * @param currentUser user whose queries to fetch.
     * @return list of all queries for currentUser.
     */
    List<UserQueryRepresentation> list(User currentUser)

    /**
     * Gets query by its identifier.
     *
     * @param id identifier of the query
     * @param currentUser user who fetches the query.
     * @return the query with the id.
     * @throws {@link NoSuchResourceException} if query with such id is not found.
     * @throws {@link AccessDeniedException} if currentUser is not the owner of the query.
     */
    UserQueryRepresentation get(Long id, User currentUser) throws NoSuchResourceException

    /**
     * Creates and saves a new user query.
     *
     * @param currentUser user for whom to create the query
     * @return the query.
     */
    UserQueryRepresentation create(UserQueryRepresentation userQuery, User currentUser)

    /**
     * Updates and saves an existing user query.
     * Only the name, bookmarked and subscribed fields are updated.
     *
     * @param id the id of the query to update
     * @param userQuery the data used to update the query with.
     * @param currentUser user for whom to update the query
     * @return the query.
     */
    UserQueryRepresentation update(Long id, UserQueryRepresentation userQuery, User currentUser)

    /**
     * Deletes query with specified id.
     *
     * @param id identifier of the query
     * @param currentUser user who deletes the query.
     * @throws {@link NoSuchResourceException} if currentUser is not the owner of the query.
     */
    void delete(Long id, User currentUser) throws NoSuchResourceException

}
