package org.transmartproject.api.server.user

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import spock.lang.Specification

import java.text.ParseException

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.READ
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.SHOW_SUMMARY_STATISTICS

class KeycloakUserResourceServiceSpec extends Specification {

    KeycloakUserResourceService testee

    def setup() {
        testee = new KeycloakUserResourceService()
        testee.authorisationChecks = Mock(AuthorisationChecks)
        testee.authorisationChecks.canPerform(_, _, _) >> true
    }

    void "test keycloak principal parsing"() {
        def requestParameters = [:]
        String clientId = 'test-client-id'
        List<GrantedAuthority> authorities = [new SimpleGrantedAuthority('ROLE_TEST')]
        boolean approved = true
        Set<String> scopes = ['oidc', 'email']
        Set<String> resourceIds = []
        String redirectUri = ''
        Set<String> responceTypes = []
        def extensionProperties = [:]


        def client = new OAuth2Request(requestParameters, clientId, authorities, approved, scopes, resourceIds,
                redirectUri, responceTypes, extensionProperties)
        def token = new UsernamePasswordAuthenticationToken('test-user', 'test-password')
        token.setDetails([
                sub  : 'test-sub',
                roles: ['ROLE_ADMIN', 'READ|STUDY1_TOKEN', 'SHOW_SUMMARY_STATISTICS|STUDY1_TOKEN', 'SHOW_SUMMARY_STATISTICS|STUDY2_TOKEN'],
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
        user.accessStudyTokenToOperations.keySet() == ['STUDY1_TOKEN', 'STUDY2_TOKEN'] as Set
        user.accessStudyTokenToOperations.get('STUDY1_TOKEN') as Set == [READ, SHOW_SUMMARY_STATISTICS] as Set
        user.accessStudyTokenToOperations.get('STUDY2_TOKEN') as Set == [SHOW_SUMMARY_STATISTICS] as Set

        when: 'we get deprecated id field'
        user.id
        then: 'unsupported exception is thrown'
        thrown(UnsupportedOperationException)

        when: 'canPeform is called'
        def canPeform = user.canPerform([:] as ProtectedOperation, [:] as ProtectedResource)
        then: 'it returns true as it is delegated to the athorisation service'
        canPeform
    }

    void 'test parse study token to operation'() {
        when:
        testee.parseStudyTokenToOperation(opToTok)
        then:
        def pe = thrown(exception)
        pe.message == message

        where:
        opToTok                      | exception                | message
        '|'                          | ParseException           | "Can't parse permission '${opToTok}'."
        'UNEXISTING_OP|STUDY1_TOKEN' | IllegalArgumentException | 'No enum constant org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.UNEXISTING_OP'
        'READ|'                      | ParseException           | "Can't parse permission '${opToTok}'."
        '|||'                        | ParseException           | "Can't parse permission '${opToTok}'."
        ''                           | ParseException           | "Can't parse permission '${opToTok}'."
    }
}
