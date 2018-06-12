package org.transmartproject.api.server.user

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.transmartproject.core.users.User
import spock.lang.Specification

import java.text.ParseException

import static org.transmartproject.core.users.AccessLevel.AGGREGATE_WITH_THRESHOLD
import static org.transmartproject.core.users.AccessLevel.OWN

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
                new SimpleGrantedAuthority('STUDY2_TOKEN|AGGREGATE_WITH_THRESHOLD'),
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
        user.studyTokenToAccessLevel.keySet() == ['STUDY1_TOKEN', 'STUDY2_TOKEN'] as Set
        user.studyTokenToAccessLevel['STUDY1_TOKEN'] == OWN
        user.studyTokenToAccessLevel['STUDY2_TOKEN'] == AGGREGATE_WITH_THRESHOLD
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
        'STUDY1_TOKEN|UNEXISTING_OP' | IllegalArgumentException | 'No enum constant org.transmartproject.core.users.AccessLevel.UNEXISTING_OP'
        '|VIEW'                      | IllegalArgumentException | "Emtpy study token: '${accLvlToTok}'."
        '|||'                        | ParseException           | "Can't parse permission '${accLvlToTok}'."
        ''                           | ParseException           | "Can't parse permission '${accLvlToTok}'."
    }
}
