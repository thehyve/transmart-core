package base

/**
 * Authentication method
 */
enum AuthMethod {
    /**
     * Keycloak - external identity provider based on Open ID Connect
     */
    OIDC,
    /**
     * Authentication using spring security plugin and OAuth2
     * Based on transmart-oauth plugin
     */
    OAuth2
}
