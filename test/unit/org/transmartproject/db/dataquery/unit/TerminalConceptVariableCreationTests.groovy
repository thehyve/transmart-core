package org.transmartproject.db.dataquery.unit

import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.clinical.ClinicalDataResourceService
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
