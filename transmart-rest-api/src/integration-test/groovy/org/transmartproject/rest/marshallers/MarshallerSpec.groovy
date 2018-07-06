/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.marshallers

import grails.rest.render.RendererRegistry
import grails.test.mixin.integration.Integration
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.transmartproject.config.RestApiTestConfiguration
import org.transmartproject.core.users.UsersResource
import spock.lang.Specification

@Integration
@DirtiesContext
@Import([RestApiTestConfiguration])
@Slf4j
abstract class MarshallerSpec extends Specification {

    @Autowired
    UsersResource usersResource

    @Autowired
    SessionFactory sessionFactory

    void testDataSetup() {
        getJson(baseURL + '/test/createData')
    }

    void setup() {
        Holders.applicationContext.getBeansOfType(RendererRegistry.class).each {
            log.info "RendererRegistry bean: ${it}"
        }
        def rendererRegistry = Holders.applicationContext.getBean('rendererRegistry')
        assert rendererRegistry.class == TransmartRendererRegistry
    }

    TestRestTemplate restTemplate = new TestRestTemplate()

    String getBaseURL() { "http://localhost:${serverPort}" }

    @Value('${local.server.port}')
    Integer serverPort

    ResponseEntity<Resource> postJson(String url, object) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        HttpEntity requestEntity = new HttpEntity(object, headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<Resource>() {})
        response
    }

    ResponseEntity<Resource> getJson(String url) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<Resource>() {})
        response
    }

    ResponseEntity<Resource> getHal(String url) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, 'application/hal+json')
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<Resource>() {})
        response
    }

}
