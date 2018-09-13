package org.transmartproject.rest.user

import org.transmartproject.core.users.User

/**
 * Holds an authenticated user.
 * Makes it easy to fetch the logged in user.
 */
interface AuthContext {
    User getUser()
}
