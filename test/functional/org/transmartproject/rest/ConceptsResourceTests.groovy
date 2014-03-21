package org.transmartproject.rest

import org.transmartproject.rest.marshallers.OntologyTermSerializationHelper

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ConceptsResourceTests extends ResourceTestCase {

    def studyId = 'study1'
    def conceptName = 'bar'

    def rootConceptPath = '\\foo\\study1\\'
    def rootConceptKey = "\\\\i2b2 main$rootConceptPath"

    def conceptPath = "${rootConceptPath}${conceptName}\\"
    def conceptKey = "${rootConceptKey}${conceptName}\\"
    //The full concept path, starting after the studyId
    def conceptId = OntologyTermSerializationHelper.pathToId(conceptName)

    def conceptListUrl = "/studies/${studyId}/concepts"
    def conceptUrl = "/studies/${studyId}/concepts/${conceptId}"
    def rootConceptUrl = "/studies/${studyId}/concepts/ROOT"


    void testIndexAsJson() {
        def result = getAsJson conceptListUrl
        assertStatus 200

        assertThat result, hasEntry(is('ontology_terms'),
            contains(hasJsonConcept())
        )
    }

    void testIndexAsHal() {
        def result = getAsHal conceptListUrl
        assertStatus 200

        assertThat result,
                hasHalIndex(
                        conceptListUrl,
                        ['ontology_terms': contains(
                                hasHalConcept()
                        )]

        )
    }

    void testShowAsJson() {
        def result = getAsJson conceptUrl
        assertStatus 200
        assertThat result, hasJsonConcept()
    }

    void testShowAsHal() {
        def result = getAsHal conceptUrl
        assertStatus 200

        assertThat result, hasHalConcept()
    }

    void testShowRootConceptAsJson() {
        def result = getAsJson rootConceptUrl
        assertStatus 200
        assertThat result, hasJsonConcept(rootConceptKey, studyId, rootConceptPath)
    }

    void testShowRootConceptAsHal() {
        def result = getAsHal rootConceptUrl
        assertStatus 200

       assertThat result, hasHalConcept(rootConceptKey, studyId, rootConceptPath, rootConceptUrl)
    }

    def hasJsonConcept(String key = conceptKey,
                       String name = conceptName,
                       String fullName = conceptPath) {
        allOf(
                hasEntry('name', name),
                hasEntry('fullName', fullName),
                hasEntry('key', key),
        )
    }

    def hasHalConcept(String key = conceptKey,
                      String name = conceptName,
                      String fullName = conceptPath,
                      String selfLink = conceptUrl) {
        allOf(
                hasJsonConcept(key, name, fullName),
                hasSelfLink(selfLink),
        )
    }

}
