package org.transmartproject.api.server.user

import org.keycloak.adapters.RefreshableKeycloakSecurityContext
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.adapters.tomcat.SimplePrincipal
import org.keycloak.representations.AccessToken
import org.keycloak.representations.idm.ClientMappingsRepresentation
import org.keycloak.representations.idm.MappingsRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.RestOperations
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User
import spock.lang.Specification

import static org.transmartproject.core.users.PatientDataAccessLevel.*

class KeycloakUserResourceServiceSpec extends Specification {

    KeycloakUserResourceService testee

    def setup() {
        testee = new KeycloakUserResourceService()
        testee.clientId = 'client2'
        testee.keycloakServerUrl = 'https://test.org/auth'
        testee.realm = 'test-realm'
    }

    void "test keycloak principal parsing"() {
        List<GrantedAuthority> authorities = [
                new SimpleGrantedAuthority('ROLE_ADMIN'),
                new SimpleGrantedAuthority('STUDY1_TOKEN|MEASUREMENTS'),
                new SimpleGrantedAuthority('STUDY2_TOKEN|COUNTS_WITH_THRESHOLD'),
        ]

        def principal = new TestingAuthenticationToken('test-sub', 'test-password', authorities)
        principal.authenticated = true

        when:
        User user = testee.getUserFromPrincipal(principal)

        then:
        user.username == 'test-sub'
        user.realName == null
        user.email == null
        user.admin
        user.studyToPatientDataAccessLevel.keySet() == ['STUDY1_TOKEN', 'STUDY2_TOKEN'] as Set
        user.studyToPatientDataAccessLevel['STUDY1_TOKEN'] == MEASUREMENTS
        user.studyToPatientDataAccessLevel['STUDY2_TOKEN'] == COUNTS_WITH_THRESHOLD
    }

    void 'work with authenticated principals only'() {
        def principal = new TestingAuthenticationToken('test', 'test-psw')
        principal.authenticated = false

        when:
        testee.getUserFromPrincipal(principal)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == 'test principal has authenticated flag set to false.'
    }

    void 'full name and email has parsed correctly from the keycloak token'() {
        def context = new RefreshableKeycloakSecurityContext()
        context.token = new AccessToken(name: 'Test User', email: 'test@test.org')
        def token = new KeycloakAuthenticationToken(new SimpleKeycloakAccount(
                new SimplePrincipal('test-principal'), [] as Set, context), true, [])

        when:
        User user = testee.getUserFromPrincipal(token)

        then:
        user.realName == 'Test User'
        user.email == 'test@test.org'
    }

    void "test fetch users with roles"() {
        testee.offlineTokenBasedRestTemplate = mockTemplate()

        when:
        def result = testee.getUsers()

        then: 'we get 3 users. username == user sub'
        result.size() == 3

        def (user1, user2, user3) = result
        user1.admin == false
        user1.username == 'user_1_sub'
        user1.realName == 'testName1 testLastName1'
        user1.email == 'user1@test.nl'
        user1.studyToPatientDataAccessLevel == ["STUDY1_TOKEN": SUMMARY]

        user2.admin == true
        user2.username == 'user_2_sub'
        user2.realName == 'testName2 testLastName2'
        user2.email == 'user2@test.nl'
        user2.studyToPatientDataAccessLevel == [:]

        user3.admin == false
        user3.username == 'user_3_sub'
        user3.realName == 'testName3 testLastName3'
        user3.email == ''
        user3.studyToPatientDataAccessLevel == ["STUDY2_TOKEN": MEASUREMENTS]

        when: 'getting user by user sub'
        def userByUsername = testee.getUserFromUsername('user_2_sub')
        then: 'the same user2 as in the list is returned'
        userByUsername == user2

        when: 'getting user by unexisting username'
        testee.getUserFromUsername('unexisting-user')
        then: 'throws an exceptionn'
        def e = thrown(NoSuchResourceException)
        e.message == "No user with 'unexisting-user' username found."

        when: 'getting users with emails'
        List<User> usersWithEmailsSpecified = testee.getUsersWithEmailSpecified()
        then: 'only 2 users have been returned'
        usersWithEmailsSpecified.size() == 2
        user1 in usersWithEmailsSpecified
        user2 in usersWithEmailsSpecified
    }

    private RestOperations mockTemplate() {
        def keycloakMockUsers = [
                new UserRepresentation(
                        id: "user_1_sub",
                        username: "user1",
                        firstName: "testName1",
                        lastName: "testLastName1",
                        email: "user1@test.nl"
                ),
                new UserRepresentation(
                        id: "user_2_sub",
                        username: "user2",
                        firstName: "testName2",
                        lastName: "testLastName2",
                        email: "user2@test.nl"
                ),
                new UserRepresentation(
                        id: "user_3_sub",
                        username: "user3",
                        firstName: "testName3",
                        lastName: "testLastName3",
                        email: ''
                )]
        def keycloakMockUser1Roles = new MappingsRepresentation(
                clientMappings: [
                        "client1": new ClientMappingsRepresentation(
                                mappings: [new RoleRepresentation(name: 'ROLE_ADMIN')]),
                        "client2": new ClientMappingsRepresentation(
                                mappings: [new RoleRepresentation(name: 'STUDY1_TOKEN|SUMMARY'),
                                           new RoleRepresentation(name: 'INVALID')]),

                ]
        )

        def keycloakMockUser2Roles = new MappingsRepresentation(
                clientMappings: [
                        "client1": new ClientMappingsRepresentation(
                                mappings: []),
                        "client2": new ClientMappingsRepresentation(
                                mappings: [new RoleRepresentation(name: 'ROLE_ADMIN')])
                ]
        )

        def keycloakMockUser3Roles = new MappingsRepresentation(
                clientMappings: [
                        "client1": new ClientMappingsRepresentation(
                                mappings: []),
                        "client2": new ClientMappingsRepresentation(
                                mappings: [new RoleRepresentation(name: 'STUDY2_TOKEN|MEASUREMENTS')]),

                ]
        )

        ResponseEntity userResponse = new ResponseEntity(keycloakMockUsers, HttpStatus.OK)
        ResponseEntity user1RolesResponse = new ResponseEntity(keycloakMockUser1Roles, HttpStatus.OK)
        ResponseEntity user2RolesResponse = new ResponseEntity(keycloakMockUser2Roles, HttpStatus.OK)
        ResponseEntity user3RolesResponse = new ResponseEntity(keycloakMockUser3Roles, HttpStatus.OK)

        Mock(RestOperations, {
            exchange("${testee.keycloakServerUrl}/admin/realms/${testee.realm}/users", HttpMethod.GET, null, KeycloakUserResourceService.userListRef) >> userResponse
            getForEntity(
                    "${testee.keycloakServerUrl}/admin/realms/${testee.realm}/users/user_1_sub/role-mappings", MappingsRepresentation.class) >> user1RolesResponse
            getForEntity(
                    "${testee.keycloakServerUrl}/admin/realms/${testee.realm}/users/user_2_sub/role-mappings", MappingsRepresentation.class) >> user2RolesResponse
            getForEntity(
                    "${testee.keycloakServerUrl}/admin/realms/${testee.realm}/users/user_3_sub/role-mappings", MappingsRepresentation.class) >> user3RolesResponse
        })
    }
}
