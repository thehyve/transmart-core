package org.transmartproject.db.user

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestFor(UsersResourceService)
@Mock(User)
class UsersResourceServiceTests {

    @Test
    void basicTest() {
        def username = 'foobar'
        def user = new User(username: username)
        user.id = -1
        user.save(failOnError: true)

        assertThat service.getUserFromUsername(username),
                is(equalTo(user))
    }

    @Test
    void testFetchUnknownUser() {
        shouldFail NoSuchResourceException, {
            service.getUserFromUsername('non_existing_user')
        }
    }

}
