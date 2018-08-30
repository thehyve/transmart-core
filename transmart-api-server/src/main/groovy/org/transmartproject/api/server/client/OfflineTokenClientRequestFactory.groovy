package org.transmartproject.api.server.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.keycloak.representations.AccessTokenResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

import java.security.cert.X509Certificate

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@CompileStatic
class OfflineTokenClientRequestFactory extends HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Value('${keycloak.resource}')
    private String clientId

    @Value('${keycloakOffline.offlineToken}')
    private String offlineToken

    @Value('${keycloak.realm}')
    private String realm

    @Value('${keycloak.auth-server-url}')
    private String keycloakServerUrl

    /**
     * Do not set this flag to true in production!
     */
    @Value('${keycloak.disable-trust-manager}')
    private Boolean keycloakDisableTrustManager

    public static final String AUTHORIZATION_HEADER = "Authorization"

    @Override
    ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
        def request = super.createRequest(uri, httpMethod)
        String accessToken = getNewAccessTokenByOfflineTokenAndClientId()
        request.headers.set(AUTHORIZATION_HEADER, "Bearer " + accessToken)
        request
    }

    /**
     * Retrieves a HttpClient that does not verify SSL certificate chains.
     * This enables the use of self-signed certificate, but makes all requests
     * made with this client insecure.
     *
     * Warning: do not use this in production!
     *
     * @return the HttpClient.
     */
    static HttpClient getHttpClientWithoutCertificateChecking() {
        log.warn "SSL certificate checking for Keycloak is disabled!"
        def acceptingTrustStrategy = { X509Certificate[] chain, String authType -> true }
        def sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build()
        HttpClients.custom()
                .setSSLContext(sslContext)
                .build()
    }

    /**
     * Creates a new RestTemplate. Depending on the value of the keycloak.disable-trust-manager property,
     * it returns a default RestTemplate (if false), or one that skips validation of SSL certificates (if true).
     *
     * @return a RestTemplate.
     */
    RestTemplate getRestTemplate() {
        def requestFactory = new HttpComponentsClientHttpRequestFactory()
        if (keycloakDisableTrustManager) {
            requestFactory.setHttpClient(httpClientWithoutCertificateChecking)
        }
        new RestTemplate(requestFactory)
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
        def httpEntity = new HttpEntity(body, headers)
        ResponseEntity<AccessTokenResponse> response = restTemplate.exchange(url,
                HttpMethod.POST, httpEntity, AccessTokenResponse.class)

        response.body.token
    }

}
