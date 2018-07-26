package tests.rest.access

import base.RESTSpec
import base.RestHelper
import com.fasterxml.jackson.databind.ObjectMapper
import config.OauthAdapter
import groovy.json.JsonSlurper
import representations.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*

class AccessCheckSpec extends RESTSpec {
    /**
     * when: "I try to fetch all studies without access token"
     * then: "I am not authorized to access the resource"
     *
     * when: "I try to fetch all studies with invalid access token"
     * then: "I am not authorized to access the resource"
     */
    def "access to resources is blocked with an invalid token"() {
        when: "I try to fetch all studies without access token"
        def request =  [
                path      : PATH_STUDIES,
                acceptType: JSON,
                user      : UNRESTRICTED_USER,
                token     : '',
                statusCode: 401
        ]
        def responseData = RestHelper.toObject get(request), ErrorResponse
        then: "I am not authorized to access the resource"
        responseData.message == "Unauthorized"

        when: "I try to fetch all studies with invalid access token"
        def requestInvalidToken = request
        requestInvalidToken.token = 'invalidToken'.bytes.encodeBase64().toString()
        def responseDataInvalidToken = RestHelper.toObject get(request), ErrorResponse

        then: "I am not authorized to access the resource"
        responseDataInvalidToken.message == "Unable to authenticate using the Authorization header"
    }

    /**
     * when: "I try to fetch all studies with a proper access token"
     * then: "I have an access to the resource"
     *
     * when: "I fabricate the token by replacing original user data with a new user"
     * then: "The signature is verified as invalid, I am not authorized to access the resource"
     */
    def "access token validation restricted per user"() {

        when: "I try to fetch all studies with a proper access token"
        def tokenForUnrestrictedUser = OauthAdapter.getToken(UNRESTRICTED_USER)

        then: "I have an access to the resource"
        get([
                path      : PATH_STUDIES,
                acceptType: JSON,
                statusCode: 200,
                token     : tokenForUnrestrictedUser
        ])


        when: "I fabricate the token by replacing original user data with a new user"
        def tokenWithReplacedUser = replaceUserInToken(tokenForUnrestrictedUser, DEFAULT_USER)
        def responseForNewUser = RestHelper.toObject get([
                path      : PATH_STUDIES,
                acceptType: JSON,
                token     : tokenWithReplacedUser,
                statusCode: 401
        ]), ErrorResponse

        then: "The signature is verified as invalid, I am not authorized to access the resource"
        responseForNewUser.error == "Unauthorized"
        responseForNewUser.message == "Unable to authenticate using the Authorization header"
    }

    private static String replaceUserInToken(String tokenForUnrestrictedUser, String newUser) {
        // Token is represented as base64 encoded: <HEADER>.<PAYLOAD>.<SIGNATURE>
        def tokenParts = tokenForUnrestrictedUser.split("\\.")
        Map tokenPayload = encodeBase64ToMap(tokenParts)

        tokenPayload.given_name = newUser
        tokenPayload.name = newUser
        tokenPayload.preferred_username = newUser
        tokenPayload.sub = getUsername(newUser)

        String newTokenPayload = decodeMapToBase64String(tokenPayload)
        tokenParts[1] = newTokenPayload

        tokenParts.join(".")
    }

    private static String decodeMapToBase64String(Map tokenPart) {
        def tokenPartString = new ObjectMapper().writeValueAsString(tokenPart)
        return tokenPartString.bytes.encodeBase64().toString()
    }

    private static Map encodeBase64ToMap(String[] tokenParts) {
        String tokenPayloadString = new String(Base64.getDecoder().decode(tokenParts[1]))
        return new JsonSlurper().parseText(tokenPayloadString) as HashMap
    }


}
