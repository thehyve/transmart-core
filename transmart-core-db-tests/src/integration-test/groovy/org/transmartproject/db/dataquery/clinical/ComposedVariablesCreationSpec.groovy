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

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import com.google.common.collect.Lists
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.clinical.variables.CategoricalVariable
import org.transmartproject.db.dataquery.clinical.variables.NormalizedLeafsVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.TestDataHelper.save

@TestMixin(ControllerUnitTestMixin)
@Integration
@Rollback
@Slf4j
class ComposedVariablesCreationSpec extends Specification {

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

    void setupData() {
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

    void testCategoricalVariableBasic() {
        setupData()
        CategoricalVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\sex\\',)

        expect: var.innerClinicalVariables allOf(
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

    void testCategoricalVariableViaConceptCode() {
        setupData()
        CategoricalVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'sex' }.code)

        expect: var.innerClinicalVariables hasSize(2)
    }

    void testCategoricalNoChildren() {
        setupData()
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: maleI2b2.fullName)
        }

        expect: throwable.message containsString('no children were found')
    }

    void testCategoricalGrandChildren() {
        setupData()
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: '\\foo\\study2\\')
        }

        expect: throwable.message containsString('has grandchildren')
    }

    void testCategoricalNonExistentConceptPath() {
        setupData()
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_path: '\\foo\\non_existent_concept_path\\')
        }

        expect: throwable.message
                containsString('Could not find concept with path')
    }

    void testCategoricalNonExistentConceptCode() {
        setupData()
        def throwable = shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.createClinicalVariable(
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    concept_code: '-3453' /* bogus */)
        }

        expect: throwable.message
                containsString('Could not find path of concept with code')
    }

    void testCategoricalRetrieveData() {
        setupData()
        CategoricalVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\sex\\',)

        def result = clinicalDataResourceService.retrieveData(
                i2b2Data.patients as Set, [var])

        // the indices list contains the flattened variables, not the original
        expect:
        result.indicesList contains(
                var.innerClinicalVariables.collect { is it } /* male, then female */)

        Lists.newArrayList(result) contains(
                facts.groupBy { it.patient }.
                        sort { it.key. /* patient */ id }.
                        collect { Patient patient, List<ObservationFact> facts ->
                            allOf(
                                    hasProperty('patient', equalTo(patient)),
                                    contains(var.innerClinicalVariables.collect { clinicalVar ->
                                        is(facts.find { it.conceptCode == clinicalVar.conceptCode }?.textValue)
                                    })
                            )
                        }
        )
    }

    /* Normalized Leaf */
    void testNormalizedLeafVariableBasic() {
        setupData()
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
        expect: var.innerClinicalVariables allOf(
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

    void testNormalizedLeafVariableConceptCodeBasic() {
        setupData()
        NormalizedLeafsVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'study2'}.code)

        println var.innerClinicalVariables
        expect: var.innerClinicalVariables allOf(
                containsInAnyOrder(
                        isA(CategoricalVariable),
                        isA(TerminalConceptVariable),
                        isA(TerminalConceptVariable)))
    }

    void testNormalizedLeafVariableOnParentOfNumericVariable() {
        setupData()
        NormalizedLeafsVariable var
        var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.fullName == '\\foo\\study1\\' }.code)

        expect: var.innerClinicalVariables contains(allOf(
                isA(TerminalConceptVariable),
                hasProperty('conceptPath', is('\\foo\\study1\\bar\\'))))
    }
}
