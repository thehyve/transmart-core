package org.transmartproject.rest

import org.transmartproject.rest.marshallers.OntologyTermSerializationHelper

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ConceptsResourceTests extends ResourceTestCase {

    def studyId = 'STUDY1'
    def conceptName = 'bar'
    def conceptFullname = '\\foo\\study1\\bar\\'
    def conceptKey = '\\\\i2b2 main\\foo\\study1\\bar\\'
    def conceptId = OntologyTermSerializationHelper.pathToId('bar') //The full concept path, starting after the studyId

    def getConceptListUrl() {
        "${baseURL}studies/${studyId}/concepts"
    }

    def getConceptUrl() {
        "${baseURL}studies/${studyId}/concepts/${conceptId}"
    }

    def getRootConceptUrl() {
        "${baseURL}studies/${studyId}/concepts/ROOT"
    }

    void testIndexAsJson() {
        def result = getAsJson getConceptListUrl()
        assertStatus 200

        assertThat result, hasEntry(is('ontology_terms'),
            contains(
                conceptJsonMatcher()
            )
        )
    }

    void testIndexAsHal() {
        def result = getAsHal getConceptListUrl()
        assertStatus 200

        assertThat result,
            allOf(
                hasEntry(
                    is('_links'),
                    hasEntry(
                        is('self'), hasEntry('href', '/studies/study1/concepts')
                    ),
                ),
                hasEntry(
                    is('_embedded'),
                    hasEntry(
                        is('ontology_terms'),
                        contains(conceptHalMatcher())
                    )
                ),
            )
    }

    void testShowAsJson() {
        def result = getAsJson getConceptUrl()
        assertStatus 200
        assertThat result, conceptJsonMatcher()
    }

    void testShowAsHal() {
        def result = getAsHal getConceptUrl()
        assertStatus 200

        assertThat result, conceptHalMatcher()
    }

    void testShowRootConceptAsJson() {
        def result = getAsJson getRootConceptUrl()
        assertStatus 200
        assertThat result, conceptJsonMatcher('\\\\i2b2 main\\foo\\study1\\', 'study1', '\\foo\\study1\\')
    }

    void testShowRootConceptAsHal() {
        def result = getAsHal getRootConceptUrl()
        assertStatus 200
        assertThat result, conceptJsonMatcher('\\\\i2b2 main\\foo\\study1\\', 'study1', '\\foo\\study1\\')
    }

    def conceptJsonMatcher(String key, String name, String fullName) {
        allOf(
                hasEntry('name', name),
                hasEntry('fullName', fullName),
                hasEntry('key', key),
        )
    }

    def conceptJsonMatcher() {
        conceptJsonMatcher(conceptKey, conceptName, conceptFullname)
    }

    def conceptHalMatcher() {
        conceptHalMatcher(conceptKey, conceptName, conceptFullname, '/studies/study1/concepts/bar')
    }

    def conceptHalMatcher(String key, String name, String fullName, String selfLink) {
        allOf(
                hasEntry('name', name),
                hasEntry('fullName', fullName),
                hasEntry('key', key),
                hasEntry(
                        is('_links'),
                        hasEntry(
                                is('self'),
                                hasEntry('href', selfLink)
                        )
                )
        )
    }

}
