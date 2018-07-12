/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.marshallers

import com.fasterxml.jackson.databind.ObjectMapper
import grails.rest.render.RendererRegistry
import grails.test.mixin.integration.Integration
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.mock.MockAuthContext
import org.transmartproject.rest.TestResource
import org.transmartproject.rest.user.AuthContext
import org.transmartproject.test.TestApplication
import spock.lang.Specification

@Integration(applicationClass = TestApplication)
@Slf4j
@CompileStatic
abstract class MarshallerSpec extends Specification {

    @Autowired
    AuthContext authContext

    @Autowired
    UsersResource usersResource

    @Autowired
    TestResource testResource

    @Autowired
    SessionFactory sessionFactory

    @Value('${local.server.port}')
    Integer serverPort

    TestRestTemplate restTemplate = new TestRestTemplate()


    void setup() {
        testResource.createTestData()
        Holders.applicationContext.getBeansOfType(RendererRegistry.class).each {
            log.info "RendererRegistry bean: ${it}"
        }
        def rendererRegistry = Holders.applicationContext.getBean('rendererRegistry')
        assert rendererRegistry.class == TransmartRendererRegistry
    }

    String getBaseURL() {
        "http://localhost:${serverPort}"
    }

    void selectUser(User user) {
        assert authContext instanceof MockAuthContext
        ((MockAuthContext)authContext).currentUser = user
    }

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

    public <T> T toObject(ResponseEntity<Resource> response, Class<T> type) {
        new ObjectMapper().readValue(response.body.inputStream, type)
    }

    String toString(ResponseEntity<Resource> response) {
        response.body.inputStream.readLines().join('\n')
    }

    void checkResponseStatus(ResponseEntity<Resource> response, HttpStatus status, User user) {
        if (response.statusCode != status) {
            def message =
                    "Unexpected status ${response.statusCode} for user ${user.username}. " +
                            "${status.value()} expected.\n" +
                            "Response: ${toString(response)}"
            log.error message
            throw new UnexpectedResultException(message)
        }
    }

}
