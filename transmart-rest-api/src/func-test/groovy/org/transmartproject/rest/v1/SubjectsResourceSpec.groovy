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
import org.springframework.http.ResponseEntity
import org.transmartproject.rest.TestData
import org.transmartproject.rest.utils.DateUtils

import static org.hamcrest.Matchers.*
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.MimeTypes.APPLICATION_HAL_JSON
import static org.transmartproject.rest.MimeTypes.APPLICATION_JSON
import static org.transmartproject.rest.utils.HalMatcherUtils.halIndexResponse
import static org.transmartproject.rest.utils.HalMatcherUtils.hasSelfLink
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson
import static spock.util.matcher.HamcrestSupport.that

class SubjectsResourceSpec extends V1ResourceSpec {

    def study = 'study_id_1'
    def defaultTrial = study.toUpperCase()
    def subjectId = -101
    def concept = 'bar'

    def subjectsPerConceptUrl = "${contextPath}/studies/${study}/concepts/${concept}/subjects"
    def subjectsPerStudyUrl = "${contextPath}/studies/${study}/subjects"

    def subjectUrl = "${contextPath}/studies/${study}/subjects/${subjectId}"
    def subjectUrl2 = "${contextPath}/studies/${study}/subjects/-102"
    def subjectUrl3 = "${contextPath}/studies/${study}/subjects/-103"

    void testShowAsJson() {
        when:
        def response = get subjectUrl
        then:
        response.statusCode == OK
        that toJson(response), hasJsonSubject()
    }

    void testShowAsHal() {
        when:
        def response = get subjectUrl, APPLICATION_HAL_JSON

        then:
        response.statusCode == OK
        that toJson(response), hasHalSubject()
    }

    void testIndexPerStudyAsJson() {
        when:
        def response = get subjectsPerStudyUrl

        then:
        response.statusCode == OK
        that toJson(response),
                hasEntry(is('subjects'),
                        containsInAnyOrder(
                                hasJsonSubject(),
                                hasJsonSubject2(),
                                hasJsonSubject3(),
                        )
                )
    }

    void testIndexPerStudyAsHal() {
        when:
        def response = get subjectsPerStudyUrl, APPLICATION_HAL_JSON

        response.statusCode == OK
        then:
        that toJson(response),
                halIndexResponse(
                        subjectsPerStudyUrl,
                        ['subjects': containsInAnyOrder(
                                hasHalSubject(),
                                hasHalSubject2(),
                                hasHalSubject3(),
                        )]
                )

    }

    void testIndexPerConceptAsJson() {
        when:
        def response = get subjectsPerConceptUrl

        then:
        response.statusCode == OK
        that toJson(response), hasEntry(is('subjects'),
                contains(
                        hasJsonSubject(),
                )
        )
    }

    void testIndexPerConceptAsHal() {
        when:
        def response = get subjectsPerConceptUrl, APPLICATION_HAL_JSON

        then:
        response.statusCode == OK
        that toJson(response),
                halIndexResponse(
                        subjectsPerConceptUrl,
                        ['subjects': contains(
                                hasHalSubject()
                        )]
                )
    }

    def subjectsPerLongConceptUrl = contextPath + '/studies/study_id_2/concepts/long path/with some$characters_/subjects'

    void testSubjectsIndexOnLongConcept() {
        when:
        def response = get subjectsPerLongConceptUrl, APPLICATION_HAL_JSON

        then:
        response.statusCode == OK
        that toJson(response), is(halIndexResponse(
                "${contextPath}/studies/study_id_2/concepts/long%20path/with%20some%24characters_/subjects",
                [subjects: any(List)]
        ))
    }

    def hasJsonSubject2() {
        hasJsonSubject(-102, defaultTrial, 'SUBJ_ID_2')
    }

    def hasJsonSubject3() {
        hasJsonSubject(-103, defaultTrial, 'SUBJ_ID_3')
    }

    def hasHalSubject2() {
        hasHalSubject(-102, defaultTrial, 'SUBJ_ID_2', subjectUrl2)
    }

    def hasHalSubject3() {
        hasHalSubject(-103, defaultTrial, 'SUBJ_ID_3', subjectUrl3)
    }

    def hasJsonSubject(int id = subjectId,
                       String trial = defaultTrial,
                       String inTrialId = 'SUBJ_ID_1') {
        allOf(
                hasEntry('id', id),
                hasEntry('sex', 'UNKNOWN'),
                hasEntry('trial', trial),
                hasEntry('inTrialId', inTrialId),
                hasEntry('religion', null),
                hasEntry('age', null),
                hasEntry('birthDate', null),
                hasEntry('maritalStatus', null),
                hasEntry('deathDate', null),
                hasEntry('race', null),
        )
    }

    def hasHalSubject(int id = subjectId,
                      String trial = defaultTrial,
                      String inTrialId = 'SUBJ_ID_1',
                      String selfLink = subjectUrl) {
        allOf(
                hasJsonSubject(id, trial, inTrialId),
                hasSelfLink(selfLink)
        )
    }

    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'

        when:
        def response = get("${contextPath}/studies/${studyName}/subjects")

        then:
        response.statusCode == NOT_FOUND
        def json = toJson(response)
        json.httpStatus == 404
        json.type == 'NoSuchResourceException'
        json.message == "No study with id '${studyName}' was found"
    }

    void testGetNonExistentSubjectForStudy() {
        def patientNum = -9999

        when:
        def response = get("${contextPath}/studies/${study}/subjects/${patientNum}")

        then:
        response.statusCode == NOT_FOUND
        def json = toJson(response)
        json.httpStatus == 404
        json.type == 'NoSuchResourceException'
        json.message == "No patient with number ${patientNum}"
    }

    void testJsonResponseContent() {
        when:
        def url = "${contextPath}/studies/${TestData.TRIAL}/subjects/${TestData.ID}"
        ResponseEntity<Resource> response = get(url)
        def result = toJson(response)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0] == APPLICATION_JSON
        that result as Map, allOf(
                hasEntry('id', TestData.ID as Integer),
                hasEntry('trial', TestData.TRIAL),
                hasEntry('inTrialId', TestData.SUBJECT_ID),
                hasEntry('birthDate', DateUtils.formatAsISO(TestData.BIRTH_DATE)),
                hasEntry('sex', TestData.SEX.name()),
                hasEntry(is('deathDate'), is(nullValue())),
                hasEntry('age', TestData.AGE as Integer),
                hasEntry('race', TestData.RACE),
                hasEntry('maritalStatus', TestData.MARITAL_STATUS),
                hasEntry('religion', TestData.RELIGION))
    }

    void testHalResponseContent() {
        when:
        def url = "${contextPath}/studies/${TestData.TRIAL}/subjects/${TestData.ID}"
        ResponseEntity<Resource> response = get(url, APPLICATION_HAL_JSON)
        def result = toJson(response)

        then:
        response.statusCode == OK
        response.headers.getFirst('Content-Type').split(';')[0] == APPLICATION_HAL_JSON
        that result as Map, allOf(
                hasEntry('age', TestData.AGE as Integer),
                hasEntry('race', TestData.RACE),
                hasEntry('maritalStatus', TestData.MARITAL_STATUS),
                // do not test the rest
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry(is('href'), is("${contextPath}/studies/${TestData.TRIAL_LC}/subjects/${TestData.ID}".toString()))
                        )
                )
        )
    }

}

