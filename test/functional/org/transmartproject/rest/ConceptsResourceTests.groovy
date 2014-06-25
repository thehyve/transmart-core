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

import org.hamcrest.Matcher

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ConceptsResourceTests extends ResourceTestCase {

    def studyId = 'study_id_1'
    def studyFolderName = 'study1'
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

    def longConceptName = 'with%some$characters_'
    def longConceptPath = "\\foo\\study2\\long path\\$longConceptName\\"
    def longConceptKey  = "\\\\i2b2 main$longConceptPath"
    def longConceptUrl  = '/studies/study_id_2/concepts/long%20path/with%25some%24characters_'

    def study2ConceptListUrl = '/studies/study_id_2/concepts'

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
        assertThat result, jsonConceptResponse(rootConceptKey, studyFolderName, rootConceptPath)
    }

    void testShowRootConceptAsHal() {
        def result = getAsHal rootConceptUrl
        assertStatus 200

       assertThat result, halConceptResponse(rootConceptKey, studyFolderName, rootConceptPath, rootConceptUrl, false)
    }

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
                                longConceptUrl,
                                false))
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
                                       String selfLink = conceptUrl,
                                       boolean highDimLink = true) {

        Map links = [self: selfLink]
        if (highDimLink) {
            links.put('highdim', "$selfLink/highdim")
        } else {
            links.put('observations', "$selfLink/observations")
        }

        allOf(
                jsonConceptResponse(key, name, fullName),
                hasLinks(links),
        )
    }

}
