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
import org.transmartproject.rest.matchers.MetadataTagsMatcher
import org.transmartproject.rest.matchers.NavigationLinksMatcher

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

class OntologyTermsResourceSpec extends ResourceSpec {

    public static final String VERSION = "v1"
    def studyId = 'study_id_1'
    def studyFolderName = 'study1'
    def partialConceptName = 'bar'

    def rootConceptPath = '\\foo\\study1\\'
    def rootConceptKey = "\\\\i2b2 main$rootConceptPath"

    def conceptPath = "${rootConceptPath}${partialConceptName}\\"
    def conceptKey = "${rootConceptKey}${partialConceptName}\\"
    //The full concept path, starting after the studyId
    def conceptId = partialConceptName

    def conceptListUrl = "/${VERSION}/studies/${studyId}/concepts"
    def conceptUrl = "/${VERSION}/studies/${studyId}/concepts/${conceptId}"
    def rootConceptUrl = "/${VERSION}/studies/${studyId}/concepts/ROOT"

    def longConceptName = 'with some$characters_'
    def longConceptPath = "\\foo\\study2\\long path\\$longConceptName\\"
    def longConceptKey = "\\\\i2b2 main$longConceptPath"
    def longConceptUrl = "/${VERSION}/studies/study_id_2/concepts/long%20path/with%20some%24characters_"

    def sexConceptUrl = "/${VERSION}/studies/study_id_2/concepts/sex"
    def femaleConceptUrl = "/${VERSION}/studies/study_id_2/concepts/sex/female"

    def study2ConceptListUrl = "/${VERSION}/studies/study_id_2/concepts"

    def study1RootConceptTags = [
            "1 name 1": "1 description 1",
            "1 name 2": "1 description 2",
    ]

    void testIndexAsJson() {
        when:
        def response = get conceptListUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, hasEntry(is('ontology_terms'),
                contains(jsonConceptResponse())
        )
    }

    void testIndexAsHal() {
        when:
        def response = get conceptListUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json,
                halIndexResponse(
                        conceptListUrl,
                        ['ontology_terms': contains(
                                halConceptResponse()
                        )]

                )
    }

    void testShowAsJson() {
        when:
        def response = get conceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, jsonConceptResponse()
    }

    void testShowAsHal() {
        when:
        def response = get conceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, halConceptResponse()
    }

    void testShowRootConceptAsJson() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, jsonConceptResponse(rootConceptKey, studyFolderName, rootConceptPath)
    }

    void testShowRootConceptAsHal() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, halConceptResponse(rootConceptKey, studyFolderName, rootConceptPath, rootConceptUrl, false)
    }

    void testPathOfLongConcept() {
        when:
        def response = get study2ConceptListUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, is(halIndexResponse(
                study2ConceptListUrl,
                ['ontology_terms': hasItem(
                        hasSelfLink(longConceptUrl))]))
    }

    void testCanHitLongConcept() {
        when:
        def response = get longConceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, is(halConceptResponse(
                longConceptKey,
                longConceptName,
                longConceptPath,
                longConceptUrl,
                false))
    }

    void testNavigableConceptRoot() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, NavigationLinksMatcher.hasNavigationLinks(rootConceptUrl, null, 'bar')
    }

    void testNavigableConceptLeaf() {
        when:
        def response = get conceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, NavigationLinksMatcher.hasNavigationLinks(conceptUrl, rootConceptUrl, null)
    }

    void testMetadataTagsAsJson() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, MetadataTagsMatcher.hasTags(study1RootConceptTags)
    }

    void testMetadataTagsAsHal() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, MetadataTagsMatcher.hasTags(study1RootConceptTags)
    }

    void testDataTypeStudy() {
        when:
        def response = get rootConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, hasEntry('type', 'STUDY')
    }

    void testDataTypeHighDimensional() {
        when:
        def response = get conceptUrl, {
            header 'Accept', contentTypeForJSON
        } //study 1/bar

        then:
        response.status == 200
        that response.json, hasEntry('type', 'HIGH_DIMENSIONAL')
    }

    void testDataTypeNumeric() {
        when:
        def response = get longConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, hasEntry('type', 'NUMERIC')
    }

    void testDataTypeCategoricalOption() {
        when:
        def response = get femaleConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, hasEntry('type', 'CATEGORICAL_OPTION')
    }

    void testDataTypeUnknown() {
        when:
        def response = get sexConceptUrl, {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json, hasEntry('type', 'UNKNOWN')
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



