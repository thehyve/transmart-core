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

import org.springframework.http.HttpStatus
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.MimeTypes

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.listOfWithOrder
import static org.thehyve.commons.test.FastMatchers.mapWith
import static org.transmartproject.rest.utils.HalMatcherUtils.hasLinks
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson
import static spock.util.matcher.HamcrestSupport.that

class ObservationsResourceSpec extends V1ResourceSpec {

    def studyId = 'STUDY_ID_1'
    def label = "\\foo\\study1\\bar\\"

    def study1BarExpectedObservations = [
            [
                    subject: [id: -103],
                    label  : label,
                    value  : null,
            ],
            [
                    subject: [id: -102],
                    label  : label,
                    value  : null,
            ],
            [
                    subject: [id: -101],
                    label  : label,
                    value  : closeTo(10.0 as Double, 0.00001 as Double),
            ],
    ]

    void setup() {
        selectUser(new MockUser('test', true))
        selectData(defaultTestData)
    }

    void testListAllObservationsForStudy() {
        when:
        def response = get("${contextPath}/studies/${studyId}/observations")

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), listOfWithOrder(study1BarExpectedObservations)
    }

    void testListAllObservationsForSubject() {
        def subjectId = -101

        when:
        def response = get("${contextPath}/studies/${studyId}/subjects/${subjectId}/observations")

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), contains(
                allOf(
                        hasEntry(is('subject'), allOf(
                                hasEntry('id', subjectId),
                                hasEntry('sex', 'UNKNOWN'),
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

        when:
        def response = get("${contextPath}/studies/${studyId}/concepts/${conceptId}/observations")

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), listOfWithOrder(study1BarExpectedObservations)
    }

    void testVariablesAreNormalized() {
        when:
        def response = get("${contextPath}/studies/study_id_2/concepts/sex/observations")

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), allOf(
                hasSize(2),
                everyItem(
                        hasEntry('label', '\\foo\\study2\\sex\\'),
                ),
                containsInAnyOrder(
                        hasEntry('value', 'male'),
                        hasEntry('value', 'female')))
    }

    void testIndexStandalonePatientSet() {
        selectUser(new MockUser('fake-user'))

        when: 'fetching all patient sets'
        def patientSetsResponse = get("${contextPath}/patient_sets")

        then: 'the response contains the clinical patient set from the test data'
        patientSetsResponse.statusCode == HttpStatus.OK
        def patientSets = (toJson(patientSetsResponse) as Map).subjects as List<Map>
        def clinicalPatientSet = patientSets.find { it.name == 'clinical-patients-set' }
        clinicalPatientSet != null

        when:
        def response = get("${contextPath}/observations?patient_sets={patient_sets}&concept_paths={concept_paths}",
                MimeTypes.APPLICATION_JSON, [patient_sets: clinicalPatientSet.id, concept_paths: '\\foo\\study1\\bar\\'])

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), listOfWithOrder(study1BarExpectedObservations)
    }

    void testIndexStandalone() {
        when:
        def response = get("${contextPath}/observations?patients={patients}&concept_paths={concept_paths}", MimeTypes.APPLICATION_JSON,
                [patients: -101, concept_paths: '\\foo\\study1\\bar\\'])

        then:
        response.statusCode == HttpStatus.OK
        that toJson(response), contains(
                hasEntry(is('subject'),
                        hasEntry('id', -101)))
    }

    void testIndexStandaloneDefaultIsNormalizedLeaves() {
        when:
        def response = get("${contextPath}/observations" +
                '?patients=-201' +
                '&patients=-202' +
                '&concept_paths=\\foo\\study2\\sex\\', MimeTypes.APPLICATION_JSON)

        then:
        response.statusCode == HttpStatus.OK
        // should be normalized
        that toJson(response), allOf(
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
        def conceptPath = '\\foo\\study2\\sex\\'

        when:
        def response = get("${contextPath}/observations?variable_type=terminal_concept_variable" +
                '&patients=-201' +
                '&patients=-202' +
                '&concept_paths=' + conceptPath)

        then:
        response.statusCode == HttpStatus.OK
        toJson(response).size() == 2
        toJson(response).every { it.label == conceptPath && it.value == null }
    }

    void testHalStandalone() {
        when:
        def response = get("${contextPath}/observations?patients={patients}&concept_paths={concept_paths}",
                MimeTypes.APPLICATION_HAL_JSON, [patients: -101, concept_paths: '\\foo\\study1\\bar\\'])

        then:
        //FIXME From time to time the status is 406 (response content is different from what's specified in the Accept)
        response.statusCode == HttpStatus.OK
        that toJson(response) as Map, allOf(
                hasLinks([:]),
                hasEntry(
                        is('_embedded'),
                        hasEntry(
                                is('observations'),
                                contains(
                                        mapWith(
                                                label: '\\foo\\study1\\bar\\',
                                                value: 10.0 as Double,
                                        )))))
    }
}
