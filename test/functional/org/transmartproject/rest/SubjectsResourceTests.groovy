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

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class SubjectsResourceTests extends ResourceTestCase {

    def study = 'study_id_1'
    def defaultTrial = study.toUpperCase()
    def subjectId = -101
    def UNKNOWN = 'UNKOWN' // funny typo here
    def concept = 'bar'

    def subjectsPerConceptUrl = "/studies/${study}/concepts/${concept}/subjects"
    def subjectsPerStudyUrl = "/studies/${study}/subjects"

    def subjectUrl = "/studies/${study}/subjects/${subjectId}"
    def subjectUrl2 = "/studies/${study}/subjects/-102"
    def subjectUrl3 = "/studies/${study}/subjects/-103"

    void testShowAsJson() {
        def result = getAsJson subjectUrl
        assertStatus 200
        assertThat result, hasJsonSubject()
    }

    void testShowAsHal() {
        def result = getAsHal subjectUrl
        assertStatus 200
        assertThat result, hasHalSubject()
    }

    void testIndexPerStudyAsJson() {

        def result = getAsJson subjectsPerStudyUrl
        assertStatus 200
        assertThat result,
                hasEntry(is('subjects'),
                        containsInAnyOrder(
                                hasJsonSubject(),
                                hasJsonSubject2(),
                                hasJsonSubject3(),
                        )
                )
    }

    void testIndexPerStudyAsHal() {

        def result = getAsHal subjectsPerStudyUrl
        assertStatus 200
        assertThat result,
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
        def result = getAsJson subjectsPerConceptUrl
        assertStatus 200

        assertThat result, hasEntry(is('subjects'),
                contains(
                        hasJsonSubject(),
                )
        )
    }

    void testIndexPerConceptAsHal() {
        def result = getAsHal subjectsPerConceptUrl
        assertStatus 200

        assertThat result,
                halIndexResponse(
                        subjectsPerConceptUrl,
                        ['subjects': contains(
                            hasHalSubject()
                        )]
                )
    }

    def subjectsPerLongConceptUrl  = '/studies/study_id_2/concepts/long%20path/with%25some%24characters_/subjects'

    void testSubjectsIndexOnLongConcept() {
        def result = getAsHal subjectsPerLongConceptUrl
        assertStatus 200

        assertThat result, is(halIndexResponse(
                subjectsPerLongConceptUrl,
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
                hasEntry('sex', UNKNOWN),
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

    /*//FIXME response contains null
    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'
        get("${baseURL}studies/${studyName}/subjects")
        assertStatus 404

        assertThat JSON, allOf(
                hasEntry('httpStatus', 404),
                hasEntry('type', 'NoSuchResourceException'),
                hasEntry('message', "No study with name '${studyName}' was found"),
        )
    }

            //FIXME response contains null
    void testGetNonExistentSubjectForStudy() {
        def patientNum = -9999
        get("${baseURL}studies/${study}/subjects/${patientNum}")
        assertStatus 404

        assertThat JSON, allOf(
                hasEntry('httpStatus', 404),
                hasEntry('type', 'NoSuchResourceException'),
                hasEntry('message', "No subject with id '${studyName}' was found"),
        )
    }*/
}

