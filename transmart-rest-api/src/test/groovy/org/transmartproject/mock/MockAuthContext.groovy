package org.transmartproject.mock

import org.transmartproject.core.users.User
import org.transmartproject.rest.user.AuthContext

/**
 * Used to specify authorised user in tests.
 */
class MockAuthContext implements AuthContext {

    User currentUser = new MockAdmin('test')

    @Override
    User getUser() {
        currentUser
    }

}
