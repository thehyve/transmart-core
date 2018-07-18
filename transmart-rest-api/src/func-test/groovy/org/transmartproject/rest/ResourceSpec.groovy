/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import grails.test.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.transmartproject.core.users.User
import org.transmartproject.mock.MockAuthContext
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.user.AuthContext
import spock.lang.Specification

import static org.transmartproject.rest.MimeTypes.APPLICATION_JSON

@Integration(applicationClass = TestApplication)
@Slf4j
abstract class ResourceSpec extends Specification {

    @Autowired
    AuthContext authContext

    @Autowired
    TestResource testResource

    void selectUser(User user) {
        assert authContext instanceof MockAuthContext
        authContext.currentUser = user
    }

    TestRestTemplate getTestRestTemplate() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(baseUrl).build()
        new TestRestTemplate(restTemplate)
    }

    void setup() {
        testResource.createTestData()
        selectUser(new MockUser('test', true))
    }

    String getBaseUrl() {
        "http://localhost:${serverPort}"
    }

    protected String getContextPath() {
        ''
    }

    @Value('${local.server.port}')
    Integer serverPort

    InputStream getAsInputStream(String relativeUrl) {
        Resource res = getTestRestTemplate()
                .getForObject(relativeUrl, Resource)
        res.inputStream
    }

    ResponseEntity<Resource> get(String relativeUrl,
                                 String acceptMimeType = APPLICATION_JSON,
                                 Map<String, Object> queryParams = [:]) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, acceptMimeType)
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = getTestRestTemplate().exchange(
                relativeUrl, HttpMethod.GET, requestEntity, Resource, queryParams)
        response
    }

    ResponseEntity<Resource> post(String relativeUrl,
                                  String acceptMimeType = APPLICATION_JSON,
                                  String contentMimeType = APPLICATION_JSON,
                                  Object object) {
        modificationRequest(relativeUrl, HttpMethod.POST,
                acceptMimeType, contentMimeType, object)
    }

    ResponseEntity<Resource> put(String relativeUrl,
                                 String acceptMimeType = APPLICATION_JSON,
                                 String contentMimeType = APPLICATION_JSON,
                                 Object object) {
        modificationRequest(relativeUrl, HttpMethod.PUT,
                acceptMimeType, contentMimeType, object)
    }

    protected ResponseEntity<Resource> modificationRequest(String relativeUrl,
                                                           HttpMethod httpMethod,
                                                           String acceptMimeType,
                                                           String contentMimeType,
                                                           Object object) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, acceptMimeType)
        headers.set(HttpHeaders.CONTENT_TYPE, contentMimeType)
        HttpEntity requestEntity = new HttpEntity(object, headers)
        ResponseEntity<Resource> response = getTestRestTemplate().exchange(
                relativeUrl, httpMethod, requestEntity, Resource)
        response
    }

    ResponseEntity<Resource> delete(String relativeUrl,
                                    String acceptMimeType = APPLICATION_JSON) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, acceptMimeType)
        HttpEntity requestEntity = new HttpEntity(headers)
        ResponseEntity<Resource> response = getTestRestTemplate().exchange(
                relativeUrl, HttpMethod.DELETE, requestEntity, Resource)
        response
    }

}
