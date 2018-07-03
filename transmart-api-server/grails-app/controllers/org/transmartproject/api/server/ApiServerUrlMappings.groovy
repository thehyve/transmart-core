package org.transmartproject.api.server

class ApiServerUrlMappings {

    static mappings = {
        "/"(redirect: "/open-api/index.html")
        "/open-api"(redirect: "/open-api/index.html")
    }

}
