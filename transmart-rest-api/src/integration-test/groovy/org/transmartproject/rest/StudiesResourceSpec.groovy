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

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

class StudiesResourceSpec extends ResourceSpec {

    public static final String VERSION = "v1"
    def childLinkHrefPath = '_embedded.ontologyTerm._links.children[0].href'
    def expectedChildLinkHrefValue = "/${VERSION}/studies/study_id_1/concepts/bar"

    void testListAllStudies() {
        when:
        def response = get("/${VERSION}/studies")
        then:
        response.status == 200
        that response.json, hasEntry(is('studies'), contains(
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
        def response = get "/$VERSION/studies", {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, halIndexResponse("/${VERSION}/studies", ['studies':
                                                             contains(
                                                                     allOf(
                                                                             hasEntry('id', 'STUDY_ID_1'),
                                                                             halIndexResponse("/${VERSION}/studies/study_id_1", ['ontologyTerm':
                                                                                                                              allOf(
                                                                                                                                      hasEntry('name', 'study1'),
                                                                                                                                      hasEntry('fullName', '\\foo\\study1\\'),
                                                                                                                                      hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                                                                                                                              )
                                                                             ])
                                                                     ),
                                                                     allOf(
                                                                             hasEntry('id', 'STUDY_ID_2'),
                                                                             halIndexResponse("/${VERSION}/studies/study_id_2", ['ontologyTerm':
                                                                                                                              allOf(
                                                                                                                                      hasEntry('name', 'study2'),
                                                                                                                                      hasEntry('fullName', '\\foo\\study2\\'),
                                                                                                                                      hasEntry('key', '\\\\i2b2 main\\foo\\study2\\'),
                                                                                                                              )
                                                                             ])
                                                                     ),
                                                                     allOf(
                                                                             hasEntry('id', 'STUDY_ID_3'),
                                                                             halIndexResponse("/${VERSION}/studies/study_id_3", ['ontologyTerm':
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
        def response = get("/$VERSION/studies/${studyId}")
        then:
        response.status == 200
        that response.json, allOf(
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
        def response = get "/$VERSION/studies/${studyId}", {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, allOf(
                hasEntry('id', 'STUDY_ID_1'),
                halIndexResponse("/$VERSION/studies/${studyId}".toLowerCase(), [
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
        def response = get "/$VERSION/studies", {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, JsonMatcher.matching(path, is(expectedChildLinkHrefValue.toString()))
    }

    void testGetStudyChildLink() {
        when:
        def response = get "/$VERSION/studies/study_id_1", {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json, JsonMatcher.matching(childLinkHrefPath, is(expectedChildLinkHrefValue.toString()))
    }


    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'

        when:
        def response = get("/$VERSION/studies/${studyName}")

        then:
        response.status == 404
        response.json.httpStatus == 404
        response.json.type == 'NoSuchResourceException'
        response.json.message == "No study with id '${studyName}' was found"
    }

}
