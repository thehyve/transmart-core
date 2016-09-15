/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@Integration
@Rollback
@Slf4j
class PatientIdListConstraintSpec extends Specification {

    AssayTestData testData = new AssayTestData()

    void setUp() {
        testData.saveAll()
    }

    void basicTest() {
        final def targetId = "SUBJ_ID_2"

        def wantedPatients = testData.patients.findAll { it.inTrialId == targetId }
        def wantedAssays = testData.assays.findAll {
            it.patient in wantedPatients
        }

        def result = new AssayQuery([
                new PatientIdListCriteriaConstraint(
                        patientIdList: [targetId]
                )
        ]).list()

        expect: result containsInAnyOrder(
                wantedAssays.collect {
                    hasSameInterfaceProperties(Assay, it)
                })
    }

    void testNonExistant() {
        def result = new AssayQuery([
                new PatientIdListCriteriaConstraint(
                        patientIdList: ["NONEXISTANT_PATIENT_567395367"]
                )
        ]).list()

        expect: result empty()
    }

    void testEmpty() {
        def result = new AssayQuery([
                new PatientIdListCriteriaConstraint(
                        patientIdList: []
                )
        ]).list()

        expect: result empty()
    }

}
