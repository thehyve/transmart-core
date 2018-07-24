package org.transmartproject.db.user

import groovy.transform.CompileStatic
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.UsersResource

import java.security.Principal
import java.util.stream.Collectors

@CompileStatic
class MockUsersResource implements UsersResource {

    List<org.transmartproject.core.users.User> users = []

    @Override
    org.transmartproject.core.users.User getUserFromUsername(String username) throws NoSuchResourceException {
        def user = users.stream().filter({org.transmartproject.core.users.User user ->
            user.username == username}).findAny()
        if (user.present) {
            return user.get()
        } else {
            throw new NoSuchResourceException("No user with username ${username}")
        }
    }

    @Override
    List<org.transmartproject.core.users.User> getUsers() {
        users
    }

    @Override
    List<org.transmartproject.core.users.User> getUsersWithEmailSpecified() {
        users.stream().filter({User user -> user.email}).collect(Collectors.toList())
    }

    @Override
    org.transmartproject.core.users.User getUserFromPrincipal(Principal principal) {
        getUserFromUsername(principal.name)
    }

}
