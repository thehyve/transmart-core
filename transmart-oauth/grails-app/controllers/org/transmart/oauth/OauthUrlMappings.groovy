package org.transmart.oauth

class OauthUrlMappings {

    static mappings = {
        group("/v2") {
            "/oauth/verify"(controller: 'oauth', action: 'verify')
            "/oauth/authorize"(uri: "/oauth/authorize.dispatch")
            "/oauth/token"(uri: "/oauth/token.dispatch")
        }

        group("/v1") {
            "/oauth/verify"(controller: 'oauth', action: 'verify')
            "/oauth/authorize"(uri: "/oauth/authorize.dispatch")
            "/oauth/token"(uri: "/oauth/token.dispatch")
        }
    }
}
