/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.marshallers

import grails.rest.render.RendererRegistry
import grails.test.mixin.integration.Integration
import grails.test.runtime.FreshRuntime
import grails.transaction.Rollback
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.transmartproject.rest.serialization.Format
import spock.lang.Specification

@FreshRuntime
@Integration
@Rollback
@Slf4j
class MarshallerSpec extends Specification {

    void setup() {
        Holders.applicationContext.getBeansOfType(RendererRegistry.class).each {
            log.info "RendererRegistry bean: ${it}"
        }
        def rendererRegistry = Holders.applicationContext.getBean('rendererRegistry')
        assert rendererRegistry.class == TransmartRendererRegistry
    }

    RestTemplate restTemplate = new TestRestTemplate()

    String getBaseURL() { "http://localhost:${serverPort}" }

    @Value('${local.server.port}')
    Integer serverPort

    ResponseEntity<Resource> postJson(url, object) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, 'application/json')
        headers.set(HttpHeaders.CONTENT_TYPE, 'application/json')
        HttpEntity requestEntity = new HttpEntity(object, headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<Resource>() {})
        response
    }

    ResponseEntity<Resource> getJson(url) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, 'application/json')
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<Resource>() {});
        response
    }

    ResponseEntity<Resource> getHal(url) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, 'application/hal+json')
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<Resource>() {});
        response
    }

    ResponseEntity<Resource> getProtobuf(url) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, Format.PROTOBUF as String)
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<Resource>() {});
        response
    }
}
