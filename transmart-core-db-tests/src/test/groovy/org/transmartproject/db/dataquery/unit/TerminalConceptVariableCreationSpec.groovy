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

import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.clinical.ClinicalDataResourceService
import org.transmartproject.db.dataquery.clinical.InnerClinicalTabularResultFactory
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class TerminalConceptVariableCreationSpec extends Specification {

    public static final String SAMPLE_CONCEPT_CODE = 'my concept code'
    public static final String SAMPLE_CONCEPT_PATH = '\\foo\\bar\\'

    ClinicalDataResourceService service = new ClinicalDataResourceService(
            clinicalVariableFactory: new ClinicalVariableFactory(),
            innerResultFactory: new InnerClinicalTabularResultFactory())

    void testCreateTerminalConceptVariableWithConceptCode() {
        def res = service.createClinicalVariable(
                concept_code: SAMPLE_CONCEPT_CODE,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)

        expect:
        res instanceof TerminalConceptVariable
        res.conceptCode == SAMPLE_CONCEPT_CODE
        res.conceptPath == null
    }

    void testCreateTerminalConceptVariableWithConceptPath() {
        def res = service.createClinicalVariable(
                concept_path: SAMPLE_CONCEPT_PATH,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)

        expect:
        res instanceof TerminalConceptVariable
        res.conceptCode == null
        res.conceptPath == SAMPLE_CONCEPT_PATH
    }

    void testSpecifyBothConceptPathAndCode() {
        when:
        service.createClinicalVariable(
                concept_path: SAMPLE_CONCEPT_PATH,
                concept_code: SAMPLE_CONCEPT_CODE,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        then:
        thrown(InvalidArgumentsException)
    }

    void testSpecifyExtraneousParameter() {
        when:
        service.createClinicalVariable(
                concept_path: SAMPLE_CONCEPT_PATH,
                foobar: 'barfoo',
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        then:
        thrown(InvalidArgumentsException)
    }

    void testSpecifyNoParameters() {
        when:
        service.createClinicalVariable([:],
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
        then:
        thrown(InvalidArgumentsException)
    }

    void testSpecifyUnrecognizedClinicalVariableType() {
        when:
        service.createClinicalVariable([:],
                'bad type of clinical variable')
        then:
        thrown(InvalidArgumentsException)
    }
}
