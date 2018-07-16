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

import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.MimeTypes
import org.transmartproject.rest.matchers.JsonMatcher

import static org.hamcrest.Matchers.*
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.MimeTypes.APPLICATION_HAL_JSON
import static org.transmartproject.rest.MimeTypes.APPLICATION_JSON
import static org.transmartproject.rest.utils.HalMatcherUtils.halIndexResponse
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson
import static spock.util.matcher.HamcrestSupport.that

class StudiesResourceSpec extends V1ResourceSpec {

    def childLinkHrefPath = '_embedded.ontologyTerm._links.children[0].href'
    def expectedChildLinkHrefValue = "${contextPath}/studies/study_id_1/concepts/bar"

    public static final String STUDY_ID = 'STUDY_ID_1'
    public static final String ONTOLOGY_TERM_NAME = 'study1'
    public static final String ONTOLOGY_KEY = '\\\\i2b2 main\\foo\\study1\\'
    public static final String ONTOLOGY_FULL_NAME = '\\foo\\study1\\'

    void setup() {
        selectUser(new MockUser('test', true))
        selectData(defaultTestData)
    }

    void testListAllStudies() {
        when:
        def response = get("${contextPath}/studies")
        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), hasEntry(is('studies'), contains(
                allOf(
                        hasEntry('id', 'STUDY_ID_1'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry('name', 'study1'),
                                hasEntry('fullName', '\\foo\\study1\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                        ))
                ),
                allOf(
                        hasEntry('id', 'STUDY_ID_2'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry('name', 'study2'),
                                hasEntry('fullName', '\\foo\\study2\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study2\\'),
                        ))
                ),
                allOf(
                        hasEntry('id', 'STUDY_ID_3'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry('name', 'study3'),
                                hasEntry('fullName', '\\foo\\study3\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study3\\'),
                        ))
                )
        ))
    }

    void testListAllStudiesAsHal() {
        when:
        def response = get "${contextPath}/studies", MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), halIndexResponse("${contextPath}/studies", ['studies':
                                                                                   contains(
                                                                                           allOf(
                                                                                                   hasEntry('id', 'STUDY_ID_1'),
                                                                                                   halIndexResponse("${contextPath}/studies/study_id_1", ['ontologyTerm':
                                                                                                                                                                  allOf(
                                                                                                                                                                          hasEntry('name', 'study1'),
                                                                                                                                                                          hasEntry('fullName', '\\foo\\study1\\'),
                                                                                                                                                                          hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                                                                                                                                                                  )
                                                                                                   ])
                                                                                           ),
                                                                                           allOf(
                                                                                                   hasEntry('id', 'STUDY_ID_2'),
                                                                                                   halIndexResponse("${contextPath}/studies/study_id_2", ['ontologyTerm':
                                                                                                                                                                  allOf(
                                                                                                                                                                          hasEntry('name', 'study2'),
                                                                                                                                                                          hasEntry('fullName', '\\foo\\study2\\'),
                                                                                                                                                                          hasEntry('key', '\\\\i2b2 main\\foo\\study2\\'),
                                                                                                                                                                  )
                                                                                                   ])
                                                                                           ),
                                                                                           allOf(
                                                                                                   hasEntry('id', 'STUDY_ID_3'),
                                                                                                   halIndexResponse("${contextPath}/studies/study_id_3", ['ontologyTerm':
                                                                                                                                                                  allOf(
                                                                                                                                                                          hasEntry('name', 'study3'),
                                                                                                                                                                          hasEntry('fullName', '\\foo\\study3\\'),
                                                                                                                                                                          hasEntry('key', '\\\\i2b2 main\\foo\\study3\\'),
                                                                                                                                                                  )
                                                                                                   ])
                                                                                           )
                                                                                   )
        ])
    }

    void testGetStudy() {
        def studyId = 'STUDY_ID_1'

        when:
        def response = get("${contextPath}/studies/${studyId}")
        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), allOf(
                hasEntry('id', 'STUDY_ID_1'),
                hasEntry(is('ontologyTerm'), allOf(
                        hasEntry('name', 'study1'),
                        hasEntry('fullName', '\\foo\\study1\\'),
                        hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                ))
        )
    }

    void testGetStudyAsHal() {
        def studyId = 'STUDY_ID_1'

        when:
        def response = get "${contextPath}/studies/${studyId}", MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), allOf(
                hasEntry('id', 'STUDY_ID_1'),
                halIndexResponse("${contextPath}/studies/${studyId}".toLowerCase(), [
                        'ontologyTerm': allOf(
                                hasEntry('name', 'study1'),
                                hasEntry('fullName', '\\foo\\study1\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                        )
                ])
        )
    }

    void testListStudiesChildLink() {
        def path = "_embedded.studies[0].$childLinkHrefPath"

        when:
        def response = get "${contextPath}/studies", MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), JsonMatcher.matching(path, is(expectedChildLinkHrefValue.toString()))
    }

    void testGetStudyChildLink() {
        when:
        def response = get "${contextPath}/studies/study_id_1", MimeTypes.APPLICATION_HAL_JSON

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), JsonMatcher.matching(childLinkHrefPath, is(expectedChildLinkHrefValue.toString()))
    }


    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'

        when:
        def response = get("${contextPath}/studies/${studyName}")

        then:
        response.statusCode == NOT_FOUND
        def json = toJson(response)
        json.httpStatus == 404
        json.type == 'NoSuchResourceException'
        json.message == "No study with id '${studyName}' was found"
    }

    void testGetStudyByOntologyKey_Json() {
        when:
        def url = "${contextPath}/studies/${STUDY_ID}?key=${ONTOLOGY_KEY}".toString()
        ResponseEntity<Resource> response = get(url)
        def result = toJson(response)

        then:
        response.statusCode == OK
        response.headers.getFirst('Content-Type').split(';')[0] == APPLICATION_JSON
        that result as Map, allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('ontologyTerm'), allOf(
                        hasEntry('name', ONTOLOGY_TERM_NAME),
                        hasEntry('fullName', ONTOLOGY_FULL_NAME),
                        hasEntry('key', ONTOLOGY_KEY),
                )))
        result.accessibleByUser == [view: true, export: true]
    }

    void testGetStudyByOntologyKey_Hal() {
        when:
        def url = "${contextPath}/studies/${STUDY_ID}?key=${ONTOLOGY_KEY}".toString()
        ResponseEntity<Resource> response = get(url, APPLICATION_HAL_JSON)
        def result = toJson(response)

        then:
        response.statusCode == OK
        response.headers.getFirst('Content-Type').split(';')[0] == APPLICATION_HAL_JSON
        that result as Map, allOf(
                hasEntry('id', STUDY_ID),
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry('href', "${contextPath}/studies/${STUDY_ID.toLowerCase()}".toString()))),
                hasEntry(is('_embedded'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry(is('_links'),
                                        hasEntry(is('self'),
                                                hasEntry('href', "${contextPath}/studies/${STUDY_ID.toLowerCase()}/concepts/ROOT".toString()))),
                                hasEntry('name', ONTOLOGY_TERM_NAME),
                                hasEntry('fullName', ONTOLOGY_FULL_NAME),
                                hasEntry('key', ONTOLOGY_KEY),
                        ))))
        result.accessibleByUser == [view: true, export: true]
    }
}
