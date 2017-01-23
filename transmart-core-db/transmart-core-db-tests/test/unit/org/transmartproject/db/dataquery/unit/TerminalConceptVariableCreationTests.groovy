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

package org.transmartproject.db.dataquery.unit

import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.clinical.ClinicalDataResourceService
import org.transmartproject.db.dataquery.clinical.InnerClinicalTabularResultFactory
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestFor(ClinicalDataResourceService)
class TerminalConceptVariableCreationTests {

    public static final String SAMPLE_CONCEPT_CODE = 'my concept code'
    public static final String SAMPLE_CONCEPT_PATH = '\\foo\\bar\\'

    @Before
    void setup() {
        defineBeans {
            clinicalVariableFactory(ClinicalVariableFactory)
            innerResultFactory(InnerClinicalTabularResultFactory)
        }
    }

    @Test
    void testCreateTerminalConceptVariableWithConceptCode() {
        def res = service.createClinicalVariable(
                concept_code: SAMPLE_CONCEPT_CODE,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)

        assertThat res, allOf(
                isA(TerminalConceptVariable),
                hasProperty('conceptCode', is(SAMPLE_CONCEPT_CODE)),
                hasProperty('conceptPath', is(nullValue())))
    }

    @Test
    void testCreateTerminalConceptVariableWithConceptPath() {
        def res = service.createClinicalVariable(
                concept_path: SAMPLE_CONCEPT_PATH,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)

        assertThat res, allOf(
                isA(TerminalConceptVariable),
                hasProperty('conceptCode', is(nullValue())),
                hasProperty('conceptPath', is(SAMPLE_CONCEPT_PATH)))
    }

    @Test
    void testSpecifyBothConceptPathAndCode() {
        shouldFail InvalidArgumentsException, {
            service.createClinicalVariable(
                    concept_path: SAMPLE_CONCEPT_PATH,
                    concept_code: SAMPLE_CONCEPT_CODE,
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        }
    }

    @Test
    void testSpecifyExtraneousParameter() {
        shouldFail InvalidArgumentsException, {
            service.createClinicalVariable(
                    concept_path: SAMPLE_CONCEPT_PATH,
                    foobar: 'barfoo',
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        }
    }

    @Test
    void testSpecifyNoParameters() {
        shouldFail InvalidArgumentsException, {
            service.createClinicalVariable([:],
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        }
    }

    @Test
    void testSpecifyUnrecognizedClinicalVariableType() {
        shouldFail InvalidArgumentsException, {
            service.createClinicalVariable([:],
                    'bad type of clinical variable')
        }
    }
}
