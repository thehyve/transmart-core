package org.transmartproject.rest

import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.users.User
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.marshallers.MarshallerSpec
import spock.lang.Shared

import static org.transmartproject.core.users.PatientDataAccessLevel.*

@Slf4j
class AccessPolicySpec extends MarshallerSpec {

    @Shared
    MockUser admin = new MockUser('admin', true)
    @Shared
    MockUser thresholdUser = new MockUser('study1User', [study1: MEASUREMENTS])
    @Shared
    MockUser study1User = new MockUser('thresholdUser', [study1: COUNTS_WITH_THRESHOLD])
    @Shared
    MockUser study2User = new MockUser('study2User', [study2: SUMMARY])
    @Shared
    MockUser study1And2User = new MockUser('study1And2User', [study1: SUMMARY, study2: SUMMARY])

    List<MockUser> getAllUsers() {
        [admin, thresholdUser, study1User, study2User, study1And2User]
    }

    void setupData() {
        // Create studies
        testResource.createTestStudy('publicStudy', true, null)
        testResource.createTestStudy('study1', false, null)
        // Create concepts

        // Create patients

        // Create observations
    }

    void 'test if mock users can access the counts that they have access to'(
            Map constraint, List<MockUser> allowedUsers) {
        given:
        setupData()
        def url = "${baseURL}/v2/observations/counts"

        expect:
        allowedUsers
        !allowedUsers.empty
        for (User user in allUsers) {
            selectUser(user)
            def body = [constraint: constraint]
            def response = postJson(url, body)
            if (allowedUsers.contains(user)) {
                expectResponse(response, HttpStatus.OK, user)
                def counts = toObject(response, Counts)
                assert counts
                // FIXME: assert counts == expectedResult (may be different for each user)
            } else {
                expectResponse(response, HttpStatus.FORBIDDEN, user)
                def error = toObject(response, Map)
                assert (error.message as String).startsWith('Access denied to study or study does not exist')
            }
        }

        where:
        constraint | allowedUsers
        [type: 'true'] | allUsers
        [type: 'study_name', studyId: 'study1'] | [admin, study1User, thresholdUser, study1And2User]
    }

}
