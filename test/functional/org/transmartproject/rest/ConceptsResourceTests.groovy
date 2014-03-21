package org.transmartproject.rest

import org.hamcrest.Matcher

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ConceptsResourceTests extends ResourceTestCase {

    def studyId = 'study1'
    def partialConceptName = 'bar'

    def rootConceptPath = '\\foo\\study1\\'
    def rootConceptKey = "\\\\i2b2 main$rootConceptPath"

    def conceptPath = "${rootConceptPath}${partialConceptName}\\"
    def conceptKey = "${rootConceptKey}${partialConceptName}\\"
    //The full concept path, starting after the studyId
    def conceptId = partialConceptName

    def conceptListUrl = "/studies/${studyId}/concepts"
    def conceptUrl = "/studies/${studyId}/concepts/${conceptId}"
    def rootConceptUrl = "/studies/${studyId}/concepts/ROOT"

    void testIndexAsJson() {
        def result = getAsJson conceptListUrl
        assertStatus 200

        assertThat result, hasEntry(is('ontology_terms'),
            contains(jsonConceptResponse())
        )
    }

    void testIndexAsHal() {
        def result = getAsHal conceptListUrl
        assertStatus 200

        assertThat result,
                halIndexResponse(
                        conceptListUrl,
                        ['ontology_terms': contains(
                                halConceptResponse()
                        )]

        )
    }

    void testShowAsJson() {
        def result = getAsJson conceptUrl
        assertStatus 200
        assertThat result, jsonConceptResponse()
    }

    void testShowAsHal() {
        def result = getAsHal conceptUrl
        assertStatus 200

        assertThat result, halConceptResponse()
    }

    void testShowRootConceptAsJson() {
        def result = getAsJson rootConceptUrl
        assertStatus 200
        assertThat result, jsonConceptResponse(rootConceptKey, studyId, rootConceptPath)
    }

    void testShowRootConceptAsHal() {
        def result = getAsHal rootConceptUrl
        assertStatus 200

       assertThat result, halConceptResponse(rootConceptKey, studyId, rootConceptPath, rootConceptUrl)
    }

    def longConceptName = 'with%some$characters_'
    def longConceptPath = "\\foo\\study2\\long path\\$longConceptName\\"
    def longConceptKey  = "\\\\i2b2 main$longConceptPath"
    def longConceptUrl  = '/studies/study2/concepts/long%20path/with%25some%24characters_'

    def study2ConceptListUrl = '/studies/study2/concepts'

    void testPathOfLongConcept() {
        def result = getAsHal study2ConceptListUrl

        assertStatus 200
        assertThat result, is(halIndexResponse(
                study2ConceptListUrl,
                ['ontology_terms': hasItem(
                        hasSelfLink(longConceptUrl))]))
    }

    void testCanHitLongConcept() {
        def result = getAsHal longConceptUrl

        assertStatus 200
        assertThat result, is(halConceptResponse(
                                longConceptKey,
                                longConceptName,
                                longConceptPath,
                                longConceptUrl))
    }

    private Matcher jsonConceptResponse(String key = conceptKey,
                                        String name = partialConceptName,
                                        String fullName = conceptPath) {
        allOf(
                hasEntry('name', name),
                hasEntry('fullName', fullName),
                hasEntry('key', key),
        )
    }

    private Matcher halConceptResponse(String key = conceptKey,
                                       String name = partialConceptName,
                                       String fullName = conceptPath,
                                       String selfLink = conceptUrl) {
        allOf(
                jsonConceptResponse(key, name, fullName),
                hasSelfLink(selfLink),
        )
    }

}
