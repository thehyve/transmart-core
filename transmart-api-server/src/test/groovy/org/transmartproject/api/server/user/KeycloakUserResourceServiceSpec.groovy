package org.transmartproject.api.server.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.transmart.api.server.KeycloakResourceService
import org.transmartproject.core.users.User
import spock.lang.Specification

import java.text.ParseException

import static org.transmartproject.core.users.PatientDataAccessLevel.*

class KeycloakUserResourceServiceSpec extends Specification {

    KeycloakUserResourceService testee

    def setup() {
        testee = new KeycloakUserResourceService()
    }

    void "test keycloak principal parsing"() {
        def requestParameters = [:]
        String clientId = 'test-client-id'
        List<GrantedAuthority> authorities = [
                new SimpleGrantedAuthority('ROLE_ADMIN'),
                new SimpleGrantedAuthority('STUDY1_TOKEN|OWN'),
                new SimpleGrantedAuthority('STUDY2_TOKEN|COUNTS_WITH_THRESHOLD'),
        ]
        boolean approved = true
        Set<String> scopes = ['oidc', 'email']
        Set<String> resourceIds = []
        String redirectUri = ''
        Set<String> responceTypes = []
        def extensionProperties = [:]

        def client = new OAuth2Request(requestParameters, clientId, authorities, approved, scopes, resourceIds,
                redirectUri, responceTypes, extensionProperties)

        def token = new UsernamePasswordAuthenticationToken('test-sub', 'test-password', authorities)
        token.setDetails([
                name : 'John Doe',
                email: 'test@mail.com',
        ])

        when:
        User user = testee.getUserFromPrincipal(new OAuth2Authentication(client, token))

        then:
        user.username == 'test-sub'
        user.realName == 'John Doe'
        user.email == 'test@mail.com'
        user.admin
        user.studyToPatientDataAccessLevel.keySet() == ['STUDY1_TOKEN', 'STUDY2_TOKEN'] as Set
        user.studyToPatientDataAccessLevel['STUDY1_TOKEN'] == MEASUREMENTS
        user.studyToPatientDataAccessLevel['STUDY2_TOKEN'] == COUNTS_WITH_THRESHOLD
    }

    void 'test parse study token to access level corner cases'() {
        when:
        testee.parseStudyTokenToAccessLevel(accLvlToTok)
        then:
        def pe = thrown(exception)
        pe.message == message

        where:
        accLvlToTok                  | exception                | message
        '|'                          | ParseException           | "Can't parse permission '${accLvlToTok}'."
        'STUDY1_TOKEN|UNEXISTING_OP' | IllegalArgumentException | 'No enum constant org.transmartproject.core.users.PatientDataAccessLevel.UNEXISTING_OP'
        '|SUMMARY'                      | IllegalArgumentException | "Empty study: '${accLvlToTok}'."
        '|||'                        | ParseException           | "Can't parse permission '${accLvlToTok}'."
        ''                           | ParseException           | "Can't parse permission '${accLvlToTok}'."
    }

    void 'test choose the higher access level in case of collision'() {
        expect:
        result == testee.buildStudyToPatientDataAccessLevel(roles)

        where:
        roles                                                            | result
        ['STUDY1|COUNTS_WITH_THRESHOLD', 'STUDY1|SUMMARY']               | ['STUDY1': SUMMARY]
        ['STUDY1|COUNTS_WITH_THRESHOLD', 'STUDY1|MEASUREMENTS', 'STUDY1|SUMMARY'] | ['STUDY1': MEASUREMENTS]
    }

    void "test fetch users with roles"() {

        def keycloakMockUsers = [
                [
                id       : "user_1",
                username : "user1",
                firstName: "testName1",
                lastName : "testLastName1",
                email    : "user1@test.nl"
        ], [
                id       : "user_2",
                username : "user2",
                firstName: "testName2",
                lastName : "testLastName2",
                email    : "user2@test.nl"
        ]]
        def keycloakMockUser1Roles = [
                "clientMappings": [
                        "client1": [
                                "mappings": [[name: 'STUDY1_TOKEN|SUMMARY'], [name: 'INVALID']]
                        ],
                        "client2": [
                                "mappings": [[name: 'ROLE_ADMIN']]
                        ]]]
        def keycloakMockUser2Roles = [
                "clientMappings": [
                        "client1": [
                                mappings: []
                        ],
                        "client2": [
                                mappings: []
                        ]]]
        ResponseEntity userResponse = new ResponseEntity(keycloakMockUsers, HttpStatus.OK)
        ResponseEntity user1RolesResponse = new ResponseEntity(keycloakMockUser1Roles, HttpStatus.OK)
        ResponseEntity user2RolesResponse = new ResponseEntity(keycloakMockUser2Roles, HttpStatus.OK)

        def keycloakResourceService = Mock(KeycloakResourceService, {
            getKeycloakResource("/admin/realms/$testee.realm/users" ) >> userResponse
            getKeycloakResource(
                    "/admin/realms/$testee.realm/users/user_1/role-mappings" ) >> user1RolesResponse
            getKeycloakResource(
                    "/admin/realms/$testee.realm/users/user_2/role-mappings" ) >> user2RolesResponse
        })
        testee.keycloakResourceService = keycloakResourceService

        when:
        def result = testee.getUsers()

        then:
        result.size() == 2

        result[0].admin == true
        result[0].username == keycloakMockUsers[0].username
        result[0].realName == "${keycloakMockUsers[0].firstName} ${keycloakMockUsers[0].lastName}"
        result[0].email == keycloakMockUsers[0].email
        result[0].studyToPatientDataAccessLevel == ["STUDY1_TOKEN": SUMMARY]

        result[1].admin == false
        result[1].username == keycloakMockUsers[1].username
        result[1].realName == "${keycloakMockUsers[1].firstName} ${keycloakMockUsers[1].lastName}"
        result[1].email == keycloakMockUsers[1].email
        result[1].studyToPatientDataAccessLevel == [:]

    }
}
