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

package org.transmartproject.db.dataquery.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import com.google.common.collect.Lists
import org.gmock.WithGMock
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.db.dataquery.clinical.variables.AcrossTrialsTerminalVariable
import org.transmartproject.db.ontology.AcrossTrialsTestData

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.AcrossTrialsTestData.MODIFIER_AGE_AT_DIAGNOSIS

@WithGMock
@Integration
@Rollback
@Slf4j
class AcrossTrialsDataRetrievalSpec extends Specification {
    public static final String AGE_AT_DIAGNOSIS_PATH =
            '\\Across Trials\\Demographics\\Age at Diagnosis\\'

    AcrossTrialsTestData testData = AcrossTrialsTestData.createDefault()

    def clinicalDataResourceService

    def sessionFactory

    void setUp() {
        testData.saveAll()

        sessionFactory.currentSession.flush()
    }

    void terminalNumericVariableTest() {
        def var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                concept_path: AGE_AT_DIAGNOSIS_PATH)

        expect: var isA(AcrossTrialsTerminalVariable)

        def result = clinicalDataResourceService.retrieveData(
                testData.patients as Set, [var])

        expect: result.indicesList contains(var)

        def innerMatchers = testData.facts.findAll {
            it.modifierCd == MODIFIER_AGE_AT_DIAGNOSIS
        }.groupBy {
            it.patient.id
        }.sort {
            it.key /* patient id */
        }.collect { patientId, facts ->
            expect: facts hasSize(1) /* sanity check */
            contains(is(facts[0].numberValue))
        }

        expect: innerMatchers is(not(empty())) /* sanity check */

        expect: Lists.newArrayList(result) contains(innerMatchers)
    }

    /* TODO
     * - test mixed across trials/regular variables
     * - test categorical data
     * - test error conditions
     */
}
