package org.transmartproject.db.user

import org.transmartproject.core.users.UsersResource

class UsersResourceService implements UsersResource {

    @Override
    org.transmartproject.core.users.User getUserFromId(Long id) {
        User.get id
    }
}
