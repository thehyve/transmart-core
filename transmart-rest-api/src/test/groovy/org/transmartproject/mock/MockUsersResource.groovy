package org.transmartproject.mock

import groovy.transform.CompileStatic
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

import java.security.Principal

@CompileStatic
class MockUsersResource implements UsersResource {

    User currentUser = new MockAdmin('test')

    @Override
    User getUserFromUsername(String username) throws NoSuchResourceException {
        if (username == currentUser.username) {
            return currentUser
        }
        throw new NoSuchResourceException()
    }

    @Override
    List<User> getUsers() {
        return [currentUser]
    }

    @Override
    List<User> getUsersWithEmailSpecified() {
        return []
    }

    @Override
    User getUserFromPrincipal(Principal principal) {
        return currentUser
    }
}
