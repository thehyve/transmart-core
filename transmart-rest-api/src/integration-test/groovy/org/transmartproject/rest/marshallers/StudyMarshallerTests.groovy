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

import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.web.mime.MimeType
import org.gmock.WithGMock
import org.junit.Test
import org.transmartproject.core.ontology.Study

import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.transmartproject.rest.test.StubStudyLoadingService.createStudy

@WithGMock
@TestMixin(IntegrationTestMixin)
class StudyMarshallerTests {

    private static final String STUDY_ID = 'TEST_STUDY'
    private static final String ONTOLOGY_TERM_NAME = 'test_study'
    private static final String ONTOLOGY_KEY = '\\\\foo bar\\foo\\test_study\\'
    private static final String ONTOLOGY_FULL_NAME = '\\foo\\test_study\\'

    Study getMockStudy() {
        createStudy(STUDY_ID, ONTOLOGY_KEY)
    }

    @Test
    void basicTest() {
        def json = mockStudy as JSON

        JsonSlurper slurper = new JsonSlurper()
        assertThat slurper.parseText(json.toString()), allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('ontologyTerm'), allOf(
                        hasEntry('name', ONTOLOGY_TERM_NAME),
                        hasEntry('fullName', ONTOLOGY_FULL_NAME),
                        hasEntry('key', ONTOLOGY_KEY),
                )))
    }

    @Test
    void testHal() {
        def json = new JSON()
        json.contentType = MimeType.HAL_JSON.name
        json.target = mockStudy

        def stringResult = json.toString()
        println stringResult

        JsonSlurper slurper = new JsonSlurper()
        assertThat slurper.parseText(stringResult), allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry('href', "/studies/${STUDY_ID.toLowerCase()}".toString()))),
                hasEntry(is('_embedded'),
                    hasEntry(is('ontologyTerm'), allOf(
                            hasEntry(is('_links'),
                                    hasEntry(is('self'),
                                            hasEntry('href', "/studies/${STUDY_ID.toLowerCase()}/concepts/ROOT".toString()))),
                            hasEntry('name', ONTOLOGY_TERM_NAME),
                            hasEntry('fullName', ONTOLOGY_FULL_NAME),
                            hasEntry('key', ONTOLOGY_KEY),
                    ))))
    }

}
