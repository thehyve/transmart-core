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

import static org.grails.web.json.JSONObject.NULL
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.listOfWithOrder
import static org.thehyve.commons.test.FastMatchers.mapWith

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

    void testVariablesAreNormalized() {
        get("/studies/study_id_2/concepts/sex/observations")
        assertStatus 200

        assertThat JSON, allOf(
                hasSize(2),
                everyItem(
                        hasEntry('label', '\\foo\\study2\\sex\\'),
                ),
                containsInAnyOrder(
                                hasEntry('value', 'male'),
                                hasEntry('value', 'female')))
    }

    void testIndexStandalonePatientSet() {
        get('/observations') {
            patient_sets = '1'
            concept_paths = '\\foo\\study1\\bar\\'
        }

        assertStatus 200

        assertThat JSON, listOfWithOrder(study1BarExpectedObservations)
    }

    void testIndexStandalone() {
        get('/observations') {
            patients = -101
            concept_paths = '\\foo\\study1\\bar\\'
        }

        assertStatus 200

        assertThat JSON, contains(
                hasEntry(is('subject'),
                        hasEntry('id', -101)))
    }

    void testIndexStandaloneDefaultIsNormalizedLeaves() {
        get(('/observations' +
                '?patients=-201' +
                '&patients=-202' +
                '&concept_paths=\\foo\\study2\\sex\\')
                .replaceAll('\\\\', '%5C'))

        assertStatus 200

        // should be normalized
        assertThat JSON, allOf(
                hasSize(2),
                containsInAnyOrder(
                        allOf(
                                hasEntry('label', '\\foo\\study2\\sex\\'),
                                hasEntry('value', 'male')),
                        allOf(
                                hasEntry('label', '\\foo\\study2\\sex\\'),
                                hasEntry('value', 'female'))))
    }

    void testIndexStandaloneDifferentVariableType() {
        get(('/observations?variable_type=terminal_concept_variable' +
                '&patients=-201' +
                '&patients=-202' +
                '&concept_paths=\\foo\\study2\\sex\\')
                .replaceAll('\\\\', '%5C'))

        assertStatus 200

        assertThat JSON, allOf(
                hasSize(2),
                everyItem(allOf(
                        hasEntry('label', '\\foo\\study2\\sex\\'),
                        hasEntry(is('value'), is(NULL)))))
    }

    void testHalStandalone() {
        getAsHal('/observations') {
            patients = -101
            concept_paths = '\\foo\\study1\\bar\\'
        }

        assertStatus 200

        allOf(
                hasLinks([:]),
                hasEntry(
                        is('_embedded'),
                        hasEntry(
                                is('observations'),
                                contains(
                                        mapWith(
                                               label: '\\foo\\study1\\bar\\',
                                                value: BigDecimal.valueOf(10.00000),
                                        )))))

    }
}
