package org.transmartproject.api.server.client

import org.keycloak.representations.AccessTokenResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class OfflineTokenClientRequestFactory extends SimpleClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Value('${keycloak.resource}')
    private String clientId

    @Value('${keycloak.offlineToken}')
    private String offlineToken

    @Value('${keycloak.realm}')
    private String realm

    @Value('${keycloak.auth-server-url}')
    private String keycloakServerUrl

    public static final String AUTHORIZATION_HEADER = "Authorization"


    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod)
        String accessToken = getNewAccessTokenByOfflineTokenAndClientId()
        connection.setRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken)
    }

    /**
     * Get access token from Keycloak based on the client_id and the offline token, which is stored in the config.
     * Offline token is a type of a classic Refresh token, but it never expires.
     * @return access token
     */
    private String getNewAccessTokenByOfflineTokenAndClientId(){

        HttpHeaders headers = new HttpHeaders()
        headers.setAccept([MediaType.APPLICATION_JSON])

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>()
        body.add("grant_type", "refresh_token")
        body.add("client_id", clientId)
        body.add("scope", "offline_access")
        body.add("refresh_token", offlineToken)

        def url = URI.create("$keycloakServerUrl/realms/$realm/protocol/openid-connect/token")
        HttpEntity<?> httpEntity = new HttpEntity<Object>(body, headers)
        ResponseEntity<Object> response = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, AccessTokenResponse.class)

        response.body.token
    }

}
