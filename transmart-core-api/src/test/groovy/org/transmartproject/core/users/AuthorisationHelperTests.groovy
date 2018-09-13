package org.transmartproject.core.users

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.users.AuthorisationHelper.copyUserWithChangedPatientDataAccessLevel
import static org.transmartproject.core.users.AuthorisationHelper.getStudyTokensForUserWithPatientDataAccessLevel
import static org.transmartproject.core.users.PatientDataAccessLevel.*

class AuthorisationHelperTests {

    User user = new SimpleUser(
            username: 'test-user',
            realName: 'Test User',
            email: 'foo@test.org',
            admin: false,
            studyToPatientDataAccessLevel: [
                    token1: COUNTS,
                    token2: COUNTS_WITH_THRESHOLD,
                    token3: SUMMARY,
                    token4: MEASUREMENTS,
            ]
    )

    @Test
    void testGetStudyTokensForUserWithPatientDataAccessLevel() {
        def tokens = getStudyTokensForUserWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)

        assertThat tokens, equalTo(['token2'] as Set)
    }

    @Test
    void testCopyUserWithChangedPatientDataAccessLevel() {
        def userCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)

        assertThat userCopy, allOf(
                not(equalTo(user)),
                hasProperty('username', equalTo('test-user')),
                hasProperty('realName', equalTo('Test User')),
                hasProperty('email', equalTo('foo@test.org')),
                hasProperty('admin', equalTo(false)),
                hasProperty('studyToPatientDataAccessLevel', equalTo(
                        [
                                token1: COUNTS,
                                token2: COUNTS,
                                token3: SUMMARY,
                                token4: MEASUREMENTS,
                        ]
                )),
        )
    }

}
