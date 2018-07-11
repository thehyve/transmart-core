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

class SubjectsResourceSpec extends ResourceSpec {

    public static final String VERSION = 'v1'
    def study = 'study_id_1'
    def defaultTrial = study.toUpperCase()
    def subjectId = -101
    def concept = 'bar'

    def subjectsPerConceptUrl = "/${VERSION}/studies/${study}/concepts/${concept}/subjects"
    def subjectsPerStudyUrl = "/${VERSION}/studies/${study}/subjects"

    def subjectUrl = "/${VERSION}/studies/${study}/subjects/${subjectId}"
    def subjectUrl2 = "/${VERSION}/studies/${study}/subjects/-102"
    def subjectUrl3 = "/${VERSION}/studies/${study}/subjects/-103"

    void testShowAsJson() {
        when:
        def response = get subjectUrl, {
            header 'Accept', contentTypeForJSON
        }
        then:
        response.status == 200
        that response.json, hasJsonSubject()
    }

    void testShowAsHal() {
        when:
        def response = get subjectUrl, {
            header 'Accept', contentTypeForHAL
        }
        then:
        response.status == 200
        that response.json, hasHalSubject()
    }

    void testIndexPerStudyAsJson() {
        when:
        def response = get subjectsPerStudyUrl, {
            header 'Accept', contentTypeForJSON
        }
        then:
        response.status == 200
        that response.json,
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
        def response = get subjectsPerStudyUrl, {
            header 'Accept', contentTypeForHAL
        }
        response.status == 200
        then:
        that response.json,
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
        def response = get subjectsPerConceptUrl, {
            header 'Accept', contentTypeForJSON
        }
        then:
        response.status == 200
        that response.json, hasEntry(is('subjects'),
                contains(
                        hasJsonSubject(),
                )
        )
    }

    void testIndexPerConceptAsHal() {
        when:
        def response = get subjectsPerConceptUrl, {
            header 'Accept', contentTypeForHAL
        }
        then:
        response.status == 200
        that response.json,
                halIndexResponse(
                        subjectsPerConceptUrl,
                        ['subjects': contains(
                                hasHalSubject()
                        )]
                )
    }

    //TODO controllers do not get decoded VERSION of url when run from functional tests
    def subjectsPerLongConceptUrl = VERSION+'/studies/study_id_2/concepts/long path/with some$characters_/subjects'

    void testSubjectsIndexOnLongConcept() {
        when:
        def response = get subjectsPerLongConceptUrl, {
            header 'Accept', contentTypeForHAL
        }
        then:
        response.status == 200
        that response.json, is(halIndexResponse(
                "/${VERSION}/studies/study_id_2/concepts/long%20path/with%20some%24characters_/subjects",
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
        def response = get("/$VERSION/studies/${studyName}/subjects")

        then:
        response.status == 404
        response.json.httpStatus == 404
        response.json.type == 'NoSuchResourceException'
        response.json.message == "No study with id '${studyName}' was found"
    }

    void testGetNonExistentSubjectForStudy() {
        def patientNum = -9999

        when:
        def response = get("/$VERSION/studies/${study}/subjects/${patientNum}")

        then:
        response.status == 404
        response.json.httpStatus == 404
        response.json.type == 'NoSuchResourceException'
        response.json.message == "No patient with number ${patientNum}"
    }
}

