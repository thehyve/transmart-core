package org.transmartproject.db.user

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.UsersResource

class UsersResourceService implements UsersResource {

    @Override
    org.transmartproject.core.users.User getUserFromUsername(String username)
            throws NoSuchResourceException {
        def user = User.findByUsername username
        if (user == null) {
            throw new NoSuchResourceException("No user with username " +
                    "$username was found")
        }

        user
    }
}
