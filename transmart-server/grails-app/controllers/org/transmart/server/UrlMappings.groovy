package org.transmart.server

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller: 'userLanding', action: 'index')
        "500"(view: '/error')
        "/open-api"(redirect: "/open-api/index.html")

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
