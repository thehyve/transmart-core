package org.transmartproject.db.dataquery.clinical

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.clinical.variables.CategoricalVariable
import org.transmartproject.db.dataquery.clinical.variables.NormalizedLeafsVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.TestDataHelper.save

@TestMixin(RuleBasedIntegrationTestMixin)
class ComposedVariablesCreationTests {

    def clinicalDataResourceService

    ConceptTestData conceptData
    I2b2Data i2b2Data
    List<ObservationFact> facts

    I2b2 getMaleI2b2() {
        conceptData.i2b2List.find { it.name == 'male'}
    }
    I2b2 getFemaleI2b2() {
        conceptData.i2b2List.find { it.name == 'female'}
    }
    I2b2 getSexI2b2() {
        conceptData.i2b2List.find { it.name == 'sex'}
    }

    @Before
    void setup() {
        conceptData = ConceptTestData.createDefault()
        i2b2Data    = I2b2Data.createDefault()
        facts       = ClinicalTestData.createDiagonalCategoricalFacts(
                3,
                [maleI2b2, femaleI2b2],
                i2b2Data.patients)

        conceptData.saveAll()
        i2b2Data.saveAll()
        save facts
    }

    /* categorical */

    @Test
    void categoricalVariableBasicTest() {
        CategoricalVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\sex\\',)

        assertThat var.innerClinicalVariables, allOf(
                everyItem(isA(TerminalConceptVariable),),
                containsInAnyOrder(
                        anyOf(
                                hasProperty('conceptPath', is(maleI2b2.fullName)),
                                hasProperty('conceptCode', is(maleI2b2.code)),
                        ),
                        anyOf(
                                hasProperty('conceptPath', is(femaleI2b2.fullName)),
                                hasProperty('conceptCode', is(femaleI2b2.code)),
                        )))
    }

    @Test
    void testCategoricalVariableViaConceptCode() {
        CategoricalVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'sex' }.code)

        assertThat var.innerClinicalVariables, hasSize(2)
    }

    @Test
    void testCategoricalNoChildren() {
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: maleI2b2.fullName)
        }

        assertThat throwable.message, containsString('no children were found')
    }

    @Test
    void testCategoricalGrandChildren() {
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: '\\foo\\study2\\')
        }

        assertThat throwable.message, containsString('has grandchildren')
    }

    @Test
    void testCategoricalNonExistentConceptPath() {
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: '\\foo\\non_existent_concept_path\\')
        }

        assertThat throwable.message,
                containsString('Could not find concept with path')
    }

    @Test
    void testCategoricalNonExistentConceptCode() {
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_code: '-3453' /* bogus */)
        }

        assertThat throwable.message,
                containsString('Could not find path of concept with code')
    }

    /* Normalized Leaf */
    @Test
    void normalizedLeafVariableBasicTest() {
        NormalizedLeafsVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_path: '\\foo\\study2\\',)

        def study1I2b2 = conceptData.i2b2List.find {
            it.fullName == '\\foo\\study2\\study1\\'
        }
        def withSomeCharsI2b2 = conceptData.i2b2List.find {
            it.name == 'with%some$characters_'
        }

        println var.innerClinicalVariables
        assertThat var.innerClinicalVariables, allOf(
                containsInAnyOrder(
                        allOf(
                                isA(CategoricalVariable),
                                hasProperty('label', is(sexI2b2.fullName))
                        ),
                        anyOf(
                                isA(TerminalConceptVariable),
                                hasProperty('conceptPath', is(withSomeCharsI2b2.fullName)),
                                hasProperty('conceptCode', is(withSomeCharsI2b2.code)),
                        ),
                        anyOf(
                                isA(TerminalConceptVariable),
                                hasProperty('conceptPath', is(study1I2b2.fullName)),
                                hasProperty('conceptCode', is(study1I2b2.code)),
                        )))
    }

    @Test
    void normalizedLeafVariableConceptCodeBasicTest() {
        NormalizedLeafsVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'study2'}.code)

        println var.innerClinicalVariables
        assertThat var.innerClinicalVariables, allOf(
                containsInAnyOrder(
                        isA(CategoricalVariable),
                        isA(TerminalConceptVariable),
                        isA(TerminalConceptVariable)))
    }

    @Test
    void normalizedLeafVariableOnParentOfNumericVariable() {
        NormalizedLeafsVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.fullName == '\\foo\\study1\\' }.code)

        assertThat var.innerClinicalVariables, contains(allOf(
                isA(TerminalConceptVariable),
                hasProperty('conceptPath', is('\\foo\\study1\\bar\\'))))
    }
}
