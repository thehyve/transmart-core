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

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.test.mixin.integration.Integration
import org.hamcrest.Matcher
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.TestRestTemplate
import org.springframework.core.io.Resource
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.mapWith

@Integration
abstract class ResourceSpec extends Specification {

    String getBaseURL() { "http://localhost:${serverPort}" }

    @Value('${local.server.port}')
    Integer serverPort

    def contentTypeForHAL = 'application/hal+json'
    def contentTypeForJSON = 'application/json'

    RestBuilder rest = new RestBuilder()

    RestTemplate restTemplate = new TestRestTemplate()

    RestResponse get(String path, Closure paramSetup = {}) {
        rest.get("${baseURL}${path}", paramSetup)
    }

    RestResponse post(String path, Closure paramSetup = {}) {
        rest.post("${baseURL}${path}", paramSetup)
    }

    InputStream getAsInputStream(String path) {
        Resource res = restTemplate.getForObject(baseURL + path, Resource.class)
        res.inputStream
    }

    /**
     * Matcher for a map entry containing _links.self[href:selfLink]
     * @param selfLink value of the expected link
     * @return matcher
     */
    def hasSelfLink(String selfLink) {
        hasEntry(
                is('_links'),
                hasEntry(
                        is('self'), hasEntry('href', selfLink)
                ),
        )
    }

    Matcher hasLinks(Map<String, String> linkMap) {
        Map expectedLinksMap = linkMap.collectEntries {
            String tempUrl = "${it.value}"
            [(it.key): ([href: tempUrl])]
        }

        hasEntry(
                is('_links'),
                mapWith(expectedLinksMap),
        )
    }

    /**
     * Generic matcher for a hal index response, expecting 2 entries:
     * - selfLink
     * - _embedded[embeddedMatcherMap*key:value]
     * @param selfLink
     * @param embeddedMatcherMap map of key to matcher elements to be expected inside '_embedded'
     * @return
     */
    def halIndexResponse(String selfLink, Map<String, Matcher> embeddedMatcherMap) {

        allOf(
                hasSelfLink(selfLink),
                hasEntry(
                        is('_embedded'),
                        allOf(
                                embeddedMatcherMap.collect {
                                    hasEntry(is(it.key), it.value)
                                }
                        )
                ),
        )
    }

}
