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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
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
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.TestDataHelper.save

@Integration
@Rollback
class ComposedVariablesCreationSpec extends Specification {

    def clinicalDataResourceService

    ConceptTestData conceptData
    I2b2Data i2b2Data
    List<ObservationFact> facts

    I2b2 getMaleI2b2() {
        conceptData.i2b2List.find { it.name == 'male' }
    }

    I2b2 getFemaleI2b2() {
        conceptData.i2b2List.find { it.name == 'female' }
    }

    I2b2 getSexI2b2() {
        conceptData.i2b2List.find { it.name == 'sex' }
    }

    void setupData() {
        conceptData = ConceptTestData.createDefault()
        i2b2Data = I2b2Data.createDefault()
        facts = ClinicalTestData.createDiagonalCategoricalFacts(
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
        CategoricalVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\sex\\',)

        expect:
        var.innerClinicalVariables.size() == 2
        var.innerClinicalVariables.every { it instanceof TerminalConceptVariable }
        var.innerClinicalVariables.find { it.conceptPath == maleI2b2.fullName || it.conceptCode == maleI2b2.code }
        var.innerClinicalVariables.find { it.conceptPath == femaleI2b2.fullName || it.conceptCode == femaleI2b2.code }
    }

    void testCategoricalVariableViaConceptCode() {
        setupData()
        CategoricalVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'sex' }.code)

        expect:
        var.innerClinicalVariables.size() == 2
    }

    void testCategoricalNoChildren() {
        setupData()

        when:
        clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: maleI2b2.fullName)
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('no children were found')
    }

    void testCategoricalGrandChildren() {
        setupData()

        when:
        clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\')
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('has grandchildren')
    }

    void testCategoricalNonExistentConceptPath() {
        setupData()

        when:
        clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\non_existent_concept_path\\')

        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('Could not find concept with path')
    }

    void testCategoricalNonExistentConceptCode() {
        setupData()

        when:
        clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_code: '-3453' /* bogus */)
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('Could not find path of concept with code')
    }

    void testCategoricalRetrieveData() {
        setupData()

        CategoricalVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.CATEGORICAL_VARIABLE,
                concept_path: '\\foo\\study2\\sex\\',)

        def expected = facts.groupBy { it.patient }.
                sort { it.key. /* patient */ id }.
                collect { Patient patient, List<ObservationFact> facts ->
                    [
                            patient,
                            var.innerClinicalVariables.collect { clinicalVar ->
                                facts.find { it.conceptCode == clinicalVar.conceptCode }?.textValue
                            }
                    ]
                }

        when:
        def result = clinicalDataResourceService.retrieveData(
                i2b2Data.patients as Set, [var])

        def list = Lists.newArrayList(result)

        // the indices list contains the flattened variables, not the original
        then:
        result.indicesList == var.innerClinicalVariables
        list.collect { [it.patient, it.collect()] } == expected
    }

    /* Normalized Leaf */

    void testNormalizedLeafVariableBasic() {
        setupData()

        def study1I2b2 = conceptData.i2b2List.find {
            it.fullName == '\\foo\\study2\\study1\\'
        }
        def withSomeCharsI2b2 = conceptData.i2b2List.find {
            it.name == 'with%some$characters_'
        }

        when:
        NormalizedLeafsVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_path: '\\foo\\study2\\',)

        then:
        var.innerClinicalVariables.size() == 3
        var.innerClinicalVariables.find { it instanceof CategoricalVariable && it.label == sexI2b2.fullName }
        var.innerClinicalVariables.find {
            it instanceof TerminalConceptVariable &&
                    (it.conceptPath == withSomeCharsI2b2.fullName || it.conceptCode == withSomeCharsI2b2.code)
        }
        var.innerClinicalVariables.find {
            it instanceof TerminalConceptVariable &&
                    (it.conceptPath == study1I2b2.fullName || it.conceptCode == study1I2b2.code)
        }
    }

    void testNormalizedLeafVariableConceptCodeBasic() {
        setupData()

        when:
        NormalizedLeafsVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.name == 'study2' }.code)

        then:
        var.innerClinicalVariables.count { it instanceof CategoricalVariable } == 1
        var.innerClinicalVariables.count { it instanceof TerminalConceptVariable } == 2
    }

    void testNormalizedLeafVariableOnParentOfNumericVariable() {
        setupData()

        when:
        NormalizedLeafsVariable var = clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: conceptData.i2b2List.find { it.fullName == '\\foo\\study1\\' }.code)

        then:
        var.innerClinicalVariables[0] instanceof TerminalConceptVariable
        var.innerClinicalVariables[0].conceptPath == '\\foo\\study1\\bar\\'
    }
}
