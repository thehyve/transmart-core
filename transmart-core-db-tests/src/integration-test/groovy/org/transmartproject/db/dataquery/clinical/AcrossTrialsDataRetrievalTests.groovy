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

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.db.dataquery.clinical.variables.AcrossTrialsTerminalVariable
import org.transmartproject.db.ontology.AcrossTrialsTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.AcrossTrialsTestData.MODIFIER_AGE_AT_DIAGNOSIS

@TestMixin(RuleBasedIntegrationTestMixin)
@WithGMock
class AcrossTrialsDataRetrievalTests {
    public static final String AGE_AT_DIAGNOSIS_PATH =
            '\\Across Trials\\Demographics\\Age at Diagnosis\\'

    AcrossTrialsTestData testData = AcrossTrialsTestData.createDefault()

    def clinicalDataResourceService

    def sessionFactory

    @Before
    void setUp() {
        testData.saveAll()

        sessionFactory.currentSession.flush()
    }

    @Test
    void terminalNumericVariableTest() {
        def var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                concept_path: AGE_AT_DIAGNOSIS_PATH)

        assertThat var, isA(AcrossTrialsTerminalVariable)

        def result = clinicalDataResourceService.retrieveData(
                testData.patients as Set, [var])

        assertThat result.indicesList, contains(var)

        def innerMatchers = testData.facts.findAll {
            it.modifierCd == MODIFIER_AGE_AT_DIAGNOSIS
        }.groupBy {
            it.patient.id
        }.sort {
            it.key /* patient id */
        }.collect { patientId, facts ->
            assertThat facts, hasSize(1) /* sanity check */
            contains(is(facts[0].numberValue))
        }

        assertThat innerMatchers, is(not(empty())) /* sanity check */

        assertThat Lists.newArrayList(result), contains(innerMatchers)
    }

    /* TODO
     * - test mixed across trials/regular variables
     * - test categorical data
     * - test error conditions
     */
}
