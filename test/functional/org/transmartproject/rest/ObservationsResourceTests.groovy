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
import static org.thehyve.commons.test.FastMatchers.listOfWithOrder

class ObservationsResourceTests extends ResourceTestCase {

    def studyId = 'STUDY_ID_1'
    def label = "\\foo\\study1\\bar\\"

    def study1BarExpectedObservations = [
            [
                    subject: [id: -103],
                    label: label,
                    value: null,
            ],
            [
                    subject: [id: -102],
                    label: label,
                    value: null,
            ],
            [
                    subject: [id: -101],
                    label: label,
                    value: closeTo(10.0 as Double, 0.00001 as Double),
            ],
    ]

    void testListAllObservationsForStudy() {
        get("${baseURL}studies/${studyId}/observations")
        assertStatus 200

        assertThat JSON, listOfWithOrder(study1BarExpectedObservations)
    }

    void testListAllObservationsForSubject() {
        def subjectId = -101
        get("${baseURL}studies/${studyId}/subjects/${subjectId}/observations")
        assertStatus 200

        assertThat JSON, contains(
                allOf(
                        hasEntry(is('subject'), allOf(
                                hasEntry('id', subjectId),
                                hasEntry('sex', 'UNKOWN'),
                                hasEntry('trial', studyId),
                                hasEntry('inTrialId', 'SUBJ_ID_1'),
                                hasEntry('religion', null),
                                hasEntry('age', null),
                                hasEntry('birthDate', null),
                                hasEntry('maritalStatus', null),
                                hasEntry('deathDate', null),
                                hasEntry('race', null),
                        )),

                        hasEntry('value', 10.0 as Double)
                )
        )
    }

    void testListAllObservationsForConcept() {
        def conceptId = 'bar'
        get("${baseURL}studies/${studyId}/concepts/${conceptId}/observations")
        assertStatus 200

        assertThat JSON, listOfWithOrder(study1BarExpectedObservations)
    }
}
