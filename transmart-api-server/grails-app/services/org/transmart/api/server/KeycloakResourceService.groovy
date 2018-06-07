package org.transmart.api.server

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Transactional
class KeycloakResourceService {

    @Value('${keycloak.clientId}')
    private String clientId

    @Value('${keycloak.offlineToken}')
    private String offlineToken

    @Value('${keycloak.serverUrl}')
    private String keycloakServerUrl

    @Value('${keycloak.realm}')
    private String realm

    /**
     * Get access token from Keycloak based on the client_id and the offline token, which is stored in the config.
     * Offline token is a type of a classic Refresh token, but it never expires.
     * @return access token
     */
    String getAccessToken(){
        RestTemplate restTemplate = new RestTemplate()

        HttpHeaders headers = new HttpHeaders()
        headers.setAccept([MediaType.APPLICATION_JSON])

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>()
        body.add("grant_type", "refresh_token")
        body.add("client_id", clientId)
        body.add("scope", "offline_access")
        body.add("refresh_token", offlineToken)

        def url = URI.create(keycloakServerUrl + "/realms/$realm/protocol/openid-connect/token")
        HttpEntity<?> httpEntity = new HttpEntity<Object>(body, headers)
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class)
        LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>)response.getBody()
        map?.access_token
    }

    /**
     * GET call to the keycloak REST API
     * using an offline token for authorization
     *
     * @param resourcePath - Keycloak REST API call url
     * @return ResponseEntity returned by Keycloak
     */
    ResponseEntity getKeycloakResource(String resourcePath){
        def accessToken = getAccessToken()
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.set("Authorization", "Bearer " + accessToken)

        RestTemplate restTemplate = new RestTemplate()

        RequestEntity<MultiValueMap<String, String>> requestEntity = new RequestEntity<>(
                headers,
                HttpMethod.GET,
                URI.create(keycloakServerUrl + resourcePath)
        )

        ResponseEntity<Object> result = restTemplate.exchange(requestEntity, Object.class)
        result
    }
}
