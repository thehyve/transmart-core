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

package org.transmartproject.rest.marshallers

import groovy.json.JsonSlurper
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

class StudyMarshallerSpec extends MarshallerSpec {

    public static final String VERSION = "v1"
    public static final String STUDY_ID = 'STUDY_ID_1'
    public static final String ONTOLOGY_TERM_NAME = 'study1'
    public static final String ONTOLOGY_KEY = '\\\\i2b2 main\\foo\\study1\\'
    public static final String ONTOLOGY_FULL_NAME = '\\foo\\study1\\'

    void basicTest() {
        when:
        def url = "${baseURL}/${VERSION}/studies/${STUDY_ID}?key=${ONTOLOGY_KEY}".toString()
        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0]  == 'application/json'
        that result as Map, allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('ontologyTerm'), allOf(
                        hasEntry('name', ONTOLOGY_TERM_NAME),
                        hasEntry('fullName', ONTOLOGY_FULL_NAME),
                        hasEntry('key', ONTOLOGY_KEY),
                )))
    }

    void testHal() {
        when:
        def url = "${baseURL}/${VERSION}/studies/${STUDY_ID}?key=${ONTOLOGY_KEY}".toString()
        ResponseEntity<Resource> response = getHal(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0]  == 'application/hal+json'
        that result as Map, allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry('href', "/${VERSION}/studies/${STUDY_ID.toLowerCase()}".toString()))),
                hasEntry(is('_embedded'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry(is('_links'),
                                        hasEntry(is('self'),
                                                hasEntry('href', "/${VERSION}/studies/${STUDY_ID.toLowerCase()}/concepts/ROOT".toString()))),
                                hasEntry('name', ONTOLOGY_TERM_NAME),
                                hasEntry('fullName', ONTOLOGY_FULL_NAME),
                                hasEntry('key', ONTOLOGY_KEY),
                        ))))
    }

}
