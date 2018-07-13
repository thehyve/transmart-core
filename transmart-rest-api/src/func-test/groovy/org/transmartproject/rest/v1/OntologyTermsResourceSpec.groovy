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

package org.transmartproject.rest.v1

import org.hamcrest.Matcher
import org.springframework.http.HttpStatus
import org.transmartproject.rest.MimeTypes
import org.transmartproject.rest.matchers.MetadataTagsMatcher
import org.transmartproject.rest.matchers.NavigationLinksMatcher

import static org.hamcrest.Matchers.*
import static org.transmartproject.rest.utils.HalMatcherUtils.*
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson
import static spock.util.matcher.HamcrestSupport.that

class OntologyTermsResourceSpec extends V1ResourceSpec {

    def studyId = 'study_id_1'
    def studyFolderName = 'study1'
    def partialConceptName = 'bar'

    def rootConceptPath = '\\foo\\study1\\'
    def rootConceptKey = "\\\\i2b2 main$rootConceptPath"

    def conceptPath = "${rootConceptPath}${partialConceptName}\\"
    def conceptKey = "${rootConceptKey}${partialConceptName}\\"
    //The full concept path, starting after the studyId
    def conceptId = partialConceptName

    def conceptListUrl = "${contextPath}/studies/${studyId}/concepts"
    def conceptUrl = "${contextPath}/studies/${studyId}/concepts/${conceptId}"
    def rootConceptUrl = "${contextPath}/studies/${studyId}/concepts/ROOT"

    def longConceptName = 'with some$characters_'
    def longConceptPath = "\\foo\\study2\\long path\\$longConceptName\\"
    def longConceptKey = "\\\\i2b2 main$longConceptPath"
    def longConceptUrl = "${contextPath}/studies/study_id_2/concepts/long%20path/with%20some%24characters_"

    def sexConceptUrl = "${contextPath}/studies/study_id_2/concepts/sex"
    def femaleConceptUrl = "${contextPath}/studies/study_id_2/concepts/sex/female"

    def study2ConceptListUrl = "${contextPath}/studies/study_id_2/concepts"

    def study1RootConceptTags = [
            "1 name 1": "1 description 1",
            "1 name 2": "1 description 2",
    ]

    void testIndexAsJson() {
        when:
        def response = get conceptListUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry(is('ontology_terms'),
                contains(jsonConceptResponse())
        )
    }

    void testIndexAsHal() {
        when:
        def response = get conceptListUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response),
                halIndexResponse(
                        conceptListUrl,
                        ['ontology_terms': contains(
                                halConceptResponse()
                        )]

                )
    }

    void testShowAsJson() {
        when:
        def response = get conceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), jsonConceptResponse()
    }

    void testShowAsHal() {
        when:
        def response = get conceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), halConceptResponse()
    }

    void testShowRootConceptAsJson() {
        when:
        def response = get rootConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), jsonConceptResponse(rootConceptKey, studyFolderName, rootConceptPath)
    }

    void testShowRootConceptAsHal() {
        when:
        def response = get rootConceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), halConceptResponse(rootConceptKey, studyFolderName, rootConceptPath, rootConceptUrl, false)
    }

    void testPathOfLongConcept() {
        when:
        def response = get study2ConceptListUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), is(halIndexResponse(
                study2ConceptListUrl,
                ['ontology_terms': hasItem(
                        hasSelfLink(longConceptUrl))]))
    }

    void testCanHitLongConcept() {
        when:
        def response = get longConceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), is(halConceptResponse(
                longConceptKey,
                longConceptName,
                longConceptPath,
                longConceptUrl,
                false))
    }

    void testNavigableConceptRoot() {
        when:
        def response = get rootConceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), NavigationLinksMatcher.hasNavigationLinks(rootConceptUrl, null, 'bar')
    }

    void testNavigableConceptLeaf() {
        when:
        def response = get conceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), NavigationLinksMatcher.hasNavigationLinks(conceptUrl, rootConceptUrl, null)
    }

    void testMetadataTagsAsJson() {
        when:
        def response = get rootConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), MetadataTagsMatcher.hasTags(study1RootConceptTags)
    }

    void testMetadataTagsAsHal() {
        when:
        def response = get rootConceptUrl, MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), MetadataTagsMatcher.hasTags(study1RootConceptTags)
    }

    void testDataTypeStudy() {
        when:
        def response = get rootConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry('type', 'STUDY')
    }

    void testDataTypeHighDimensional() {
        when:
        def response = get conceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry('type', 'HIGH_DIMENSIONAL')
    }

    void testDataTypeNumeric() {
        when:
        def response = get longConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry('type', 'NUMERIC')
    }

    void testDataTypeCategoricalOption() {
        when:
        def response = get femaleConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry('type', 'CATEGORICAL_OPTION')
    }

    void testDataTypeUnknown() {
        when:
        def response = get sexConceptUrl

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry('type', 'UNKNOWN')
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



