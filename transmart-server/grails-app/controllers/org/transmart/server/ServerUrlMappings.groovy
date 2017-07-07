package org.transmart.server

class ServerUrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {}
        }
        "/"(redirect: "/userLanding")
        "/open-api"(redirect: "/open-api/index.html")
    }
}
