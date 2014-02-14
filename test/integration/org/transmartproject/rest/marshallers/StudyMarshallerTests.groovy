package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.gmock.WithGMock
import org.junit.Test
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@WithGMock
class StudyMarshallerTests {


    private static final String STUDY_NAME = 'TEST_STUDY'
    private static final String ONTOLOGY_TERM_NAME = 'test_study'
    private static final String ONTOLOGY_KEY = '\\\\foo bar\\foo\\test_study\\'
    private static final String ONTOLOGY_FULL_NAME = '\\foo\\test_study\\'

    Study getMockStudy() {
        [
                getName: { -> STUDY_NAME },
                getOntologyTerm: { ->
                    [
                            getName: { -> ONTOLOGY_TERM_NAME },
                            getFullName: { -> ONTOLOGY_FULL_NAME },
                            getKey: { ->  ONTOLOGY_KEY }
                    ] as OntologyTerm
                }
        ] as Study
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

}
