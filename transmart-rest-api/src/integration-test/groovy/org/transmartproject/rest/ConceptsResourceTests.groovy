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

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher

import static spock.util.matcher.HamcrestSupport.that
import static org.hamcrest.Matchers.*

class ConceptsResourceTests extends ResourceSpec {

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

    def longConceptName = 'with some$characters_'
    def longConceptPath = "\\foo\\study2\\long path\\$longConceptName\\"
    def longConceptKey = "\\\\i2b2 main$longConceptPath"
    def longConceptUrl = '/studies/study_id_2/concepts/long%20path/with%20some%24characters_'

    def sexConceptUrl = '/studies/study_id_2/concepts/sex'
    def femaleConceptUrl = '/studies/study_id_2/concepts/sex/female'

    def study2ConceptListUrl = '/studies/study_id_2/concepts'

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
        // unfortunately the test data has no concept with the study
        // visual attribute. This is detected as a study because the ontology
        // term is the same as the one studyLoadingService.study.ot returns
        // See OntologyTermWrapper
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

class NavigationLinksMatcher extends DiagnosingMatcher<JSONObject> {

    Matcher selfLinkMatcher
    Matcher parentLinkMatcher
    Matcher childrenMatcher

    static NavigationLinksMatcher hasNavigationLinks(String selfLink,
                                                     String parentLink,
                                                     String... children) {

        def slm = LinkMatcher.hasLink(selfLink, null)
        def plm = parentLink ? LinkMatcher.hasLink(parentLink, null) : null

        def baseUrl = selfLink.endsWith('ROOT') ? selfLink.substring(0, selfLink.indexOf('/ROOT')) : selfLink
        List<Matcher> childrenMatchers = children.collect {
            LinkMatcher.hasLink("$baseUrl/$it", it)
        }

        def cm = childrenMatchers.isEmpty() ? null : containsInAnyOrder(childrenMatchers.collect { it })

        new NavigationLinksMatcher(selfLinkMatcher: slm, parentLinkMatcher: plm, childrenMatcher: cm)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        if (!obj.has('_links')) {
            mismatchDescription.appendText("no '_links' was found")
            return false
        }
        JSONObject links = obj.getJSONObject('_links')

        JSONObject self = links.getJSONObject('self')
        if (!self) {
            mismatchDescription.appendText("no 'self' was found")
            return false
        }

        boolean result = selfLinkMatcher.matches(self, mismatchDescription)

        if (!result) {
            return false
        }

        if (parentLinkMatcher) {
            JSONObject parent = links.getJSONObject('parent')
            if (!parent) {
                mismatchDescription.appendText("no 'parent' was found")
                result = false
            } else {
                result = parentLinkMatcher.matches(parent, mismatchDescription)
            }
        }

        if (!result) {
            return false
        }

        def hasChildren = links.has('children')

        if (childrenMatcher) {
            if (hasChildren) {
                JSONArray children = links.getJSONArray('children')
                result = childrenMatcher.matches(children)
            } else {
                mismatchDescription.appendText("no 'children' was found")
                result = false
            }
        } else if (hasChildren) {
            mismatchDescription.appendText("not expected 'children' was found")
            result = false
        }

        return result
    }

    @Override
    void describeTo(Description description) {
        description.appendText("'self' with ")
        selfLinkMatcher.describeTo(description)
        if (parentLinkMatcher) {
            description.appendText(" and 'parent' with ")
            parentLinkMatcher.describeTo(description)
        }
        if (childrenMatcher) {
            description.appendText(" and 'children' with ")
            childrenMatcher.describeTo(description)
        }
    }
}

class LinkMatcher extends DiagnosingMatcher<JSONObject> {

    String expectedUrl
    String expectedTitle

    static LinkMatcher hasLink(String url, String title) {
        new LinkMatcher(expectedUrl: url, expectedTitle: title)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        String url = obj.get('href')

        if (expectedUrl != url) {
            mismatchDescription.appendText("link href did not match:")
            mismatchDescription.appendText(" expecting ").appendValue(expectedUrl)
            mismatchDescription.appendText(" was ").appendValue(url)
            return false
        }

        if (expectedTitle) {
            String title = obj.get('title')
            if (expectedTitle != title) {
                mismatchDescription.appendText("link title did not match:")
                mismatchDescription.appendText(" expecting ").appendValue(expectedTitle)
                mismatchDescription.appendText(" was ").appendValue(title)
                return false
            }
        }

        return true
    }

    @Override
    void describeTo(Description description) {
        description.appendText('link with href ').appendValue(expectedUrl)
        if (expectedTitle) {
            description.appendText(' and with title ').appendValue(expectedTitle)
        }
    }

}

class MetadataTagsMatcher extends DiagnosingMatcher<JSONObject> {
    Map<String, String> expectedTags

    static MetadataTagsMatcher hasTags(Map<String, String> tags) {
        new MetadataTagsMatcher(expectedTags: tags)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        def hasMetadata = obj.has('metadata')

        if (expectedTags.isEmpty()) {
            if (hasMetadata) {
                mismatchDescription.appendText(" was not expecting any metadata tags, but got them")
                return false
            } else {
                return true
            }
        }

        JSONObject md = obj.getJSONObject('metadata')

        Map map = md as Map
        if (map != expectedTags) {
            mismatchDescription.appendText(" tags did not match")
            mismatchDescription.appendText(" expecting ").appendValue(expectedTags)
            mismatchDescription.appendText(" was ").appendValue(map)
            return false
        }

        return true
    }

    @Override
    void describeTo(Description description) {
        description.appendText(' concept with metadata tags ').appendValue(expectedTags)
    }
}
