package org.transmartproject.api.server.client

import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

class CustomRestTemplate extends RestTemplate implements RestOperations {
    CustomRestTemplate(CustomClientRequestFactory factory) {
        super(factory)
    }

}
