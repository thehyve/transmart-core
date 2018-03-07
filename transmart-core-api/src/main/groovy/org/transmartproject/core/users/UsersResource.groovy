package org.transmartproject.core.users

import org.transmartproject.core.exceptions.NoSuchResourceException

/**
 * Resource related with users.
 */
interface UsersResource {

    /**
     * Retrieves the user with the username if it exists, throws an exception otherwise.
     * @param username the username of the user.
     * @return the user entity.
     * @throws NoSuchResourceException iff no user exists with the username.
     */
    User getUserFromUsername(String username) throws NoSuchResourceException

    /**
     * Retrieves the list of all users.
     * @return the list of all users.
     */
    List<User> getUsers()

    /**
     * Retrieves the list of all users that have an email specified.
     * @return the list of users.
     */
    List<User> getUsersWithEmailSpecified()
}
