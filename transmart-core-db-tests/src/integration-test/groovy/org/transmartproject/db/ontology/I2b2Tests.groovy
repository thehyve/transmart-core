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

package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import grails.util.Holders
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess

@TestMixin(RuleBasedIntegrationTestMixin)
class I2b2Tests {

    @Before
    void setUp() {
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 table code', tableName: 'i2b2')
        addI2b2(level: 0, fullName: '\\foo\\', name: 'foo',
                cComment: 'trial:STUDY_ID')
        addI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'var',
                cVisualattributes: 'FH', cComment: 'trial:STUDY_ID')
        addI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto',
                cComment: 'trial:STUDY_ID')
        addI2b2(level: 2, fullName: '\\foo\\xpto\\bar\\', name: 'bar',
                cComment: 'trial:STUDY_ID')

        addI2b2(level: 3, fullName: '\\foo\\xpto\\bar\\jar\\', name: 'jar')
        addI2b2(level: 3, fullName: '\\foo\\xpto\\bar\\binks\\', name: 'binks')
    }

    @Test
    void testGetVisualAttributes() {
        def terms = I2b2.withCriteria { eq 'level', 1 }

        //currently testing against postgres database,
        // which has already a matching row there
        assertThat terms, hasSize(greaterThanOrEqualTo(2))
        //assertThat terms, hasSize(2)

        assertThat terms, hasItem(allOf(
                hasProperty('name', equalTo('var')),
                hasProperty('visualAttributes', containsInAnyOrder(
                        OntologyTerm.VisualAttributes.FOLDER,
                        OntologyTerm.VisualAttributes.HIDDEN
                ))
        ))
    }

    @Test
    void testGetTableCode() {
        I2b2 bar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\') }

        assertThat(bar.conceptKey, is(equalTo(
                new ConceptKey('i2b2 table code', '\\foo\\xpto\\bar\\'))))
    }

    @Test
    void testGetChildren() {
        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto\\') }
        xpto.setTableCode('i2b2 table code OOOO')

        assertThat xpto, is(notNullValue())

        def children = xpto.children
        assertThat(children, allOf(
                hasSize(1),
                contains(allOf(
                        hasProperty('fullName', equalTo('\\foo\\xpto\\bar\\')),
                        //table code is copied from parent:
                        hasProperty('conceptKey', equalTo(new ConceptKey
                        ('\\\\i2b2 table code OOOO\\foo\\xpto\\bar\\')))
                ))))
    }

    @Test
    void testGetAllDescendants() {

        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto\\') }
        assertThat xpto, is(notNullValue())

        def children = xpto.allDescendants
        assertThat(children,  allOf (
                hasSize(3),
                contains(
                        hasProperty('name', equalTo('bar')),
                        hasProperty('name', equalTo('binks')),
                        hasProperty('name', equalTo('jar')),
                )
        ))
    }

    @Test
    void testGetStudy() {
        I2b2 bar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\') }
        I2b2 foo = I2b2.find { eq('fullName', '\\foo\\') }

        Study study = bar.study
        assertThat study, allOf(
                hasProperty('id', is('STUDY_ID')),
                hasProperty('ontologyTerm', is(foo)),
        )
    }

    @Test
    void testGetStudyNull() {
        I2b2 jar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\jar\\') }
        assertThat jar.study, is(nullValue())
    }

    @Test
    void testGetPatients() {

        ConceptTestData.addTableAccess(level: 0, fullName: '\\test\\', name: 'test',
                tableCode: 'i2b2 main', tableName: 'i2b2')

        def concepts = ConceptTestData.createMultipleI2B2(3)
        def patients = I2b2Data.createTestPatients(5, -100, 'SAMPLE TRIAL')
        def observations = createObservations(concepts, patients)

        HighDimTestData.save concepts
        HighDimTestData.save patients
        HighDimTestData.save observations
        HighDimTestData.save ConceptTestData.createConceptDimensions(concepts)

        def result = concepts[0].getPatients()
        assertThat result, containsInAnyOrder(
                patients[0],
                patients[1],
                patients[2]
        )

        def result2 = concepts[1].getPatients()
        assertThat result2, containsInAnyOrder(
                patients[2],
                patients[3]
        )

        def result3 = concepts[2].getPatients()
        assertThat result3, containsInAnyOrder(
            //empty
        )

    }

    @Test
    void testSynonymIsTransient() {
        GrailsDomainClass domainClass =
                Holders.grailsApplication.getDomainClass('org.transmartproject.db.ontology.I2b2')
        GrailsDomainClassProperty synonymProperty =
                domainClass.getPropertyByName('synonym')

        assertThat synonymProperty.isPersistent(), is(false)
    }

    @Test
    void testSynonym() {
        I2b2 jar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\jar\\') }
        assertThat jar, hasProperty('synonym', is(false))

        jar.cSynonymCd = 'Y'
        assertThat jar, hasProperty('synonym', is(true))
    }

    def createObservations(List<I2b2> concepts, List<Patient> patients) {
        List result = []
        //concept 0 with patients 0, 1 and 2
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[0], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[1], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[2], 1, 2)
        //concept 1 with patients 2 and 3
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[2], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[3], 1, 2)
        result
    }

}
