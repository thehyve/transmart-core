package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.web.mime.MimeType
import org.gmock.WithGMock
import org.junit.Test
import org.transmartproject.core.ontology.Study

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.rest.test.StubStudyLoadingService.createStudy

@WithGMock
@TestMixin(IntegrationTestMixin)
class StudyMarshallerTests {

    private static final String STUDY_NAME = 'TEST_STUDY'
    private static final String ONTOLOGY_TERM_NAME = 'test_study'
    private static final String ONTOLOGY_KEY = '\\\\foo bar\\foo\\test_study\\'
    private static final String ONTOLOGY_FULL_NAME = '\\foo\\test_study\\'

    Study getMockStudy() {
        createStudy(STUDY_NAME, ONTOLOGY_KEY)
    }

    @Test
    void basicTest() {
        def json = mockStudy as JSON

        JsonSlurper slurper = new JsonSlurper()
        assertThat slurper.parseText(json.toString()), allOf(
                hasEntry('name', STUDY_NAME),
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
                hasEntry('name', STUDY_NAME),
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry('href', "/studies/$ONTOLOGY_TERM_NAME".toString()))),
                hasEntry(is('_embedded'),
                    hasEntry(is('ontologyTerm'), allOf(
                            hasEntry(is('_links'),
                                    hasEntry(is('self'),
                                            hasEntry('href', "/studies/$ONTOLOGY_TERM_NAME/concepts/ROOT".toString()))),
                            hasEntry('name', ONTOLOGY_TERM_NAME),
                            hasEntry('fullName', ONTOLOGY_FULL_NAME),
                            hasEntry('key', ONTOLOGY_KEY),
                    ))))
    }

}
