/*
 * Copyright (c) 2016 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This code is licensed under the GNU General Public License,
 * version 3, or (at your option) any later version.
 */

package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.web.mime.MimeType
import org.junit.Test
import org.transmartproject.core.biomarker.BioMarker

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(IntegrationTestMixin)
class BioMarkerMarshallerTests {
    static final TYPE = 'GENE'
    static final EXTERNAL_ID = 'FBM500'
    static final SOURCECODE = 'Mock biomarker db'
    static final NAME = 'BioMarker 1'
    static final DESCRIPTION = "Fake biomarker"
    static final ORGANISM = "HOMO SAPIENS"

    def getMockBioMarker() {
        new BioMarker() {
            Long id = -50
            String type = TYPE
            String externalId = EXTERNAL_ID
            String sourceCode = SOURCECODE
            String name = NAME
            String description = DESCRIPTION
            String organism = ORGANISM
        }
    }

    @Test
    void basicTest() {
        def json = mockBioMarker as JSON
        def parsed = new JsonSlurper().parseText(json.toString())

        assertBasicProperties(parsed)
    }

    def assertBasicProperties(biomarker) {
        assertThat biomarker, allOf(
                not(hasKey('id')),
                hasEntry('type', TYPE),
                hasEntry('externalId', EXTERNAL_ID),
                hasEntry('sourceCode', SOURCECODE),
                hasEntry('name', NAME),
                hasEntry('description', DESCRIPTION),
                hasEntry('organism', ORGANISM)
        )
    }

    @Test
    void testHal() {
        def json = new JSON()
        json.contentType = MimeType.HAL_JSON.name
        json.target = mockBioMarker

        def stringResult = json.toString()
        def parsed = new JsonSlurper().parseText(stringResult)

        assertBasicProperties(parsed)
        assertThat parsed, hasEntry('_links', [:])
    }

}
