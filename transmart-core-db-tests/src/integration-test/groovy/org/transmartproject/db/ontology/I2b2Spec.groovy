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

import grails.core.GrailsDomainClassProperty
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.Holders
import org.grails.core.DefaultGrailsDomainClass
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.StudyTestData
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.TrialVisit
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess

@Integration
@Rollback
class I2b2Spec extends Specification {

    @Autowired SessionFactory sessionFactory

    void setupData() {
        TestData.prepareCleanDatabase()

        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 table code', tableName: 'i2b2')
        addI2b2(level: 0, fullName: '\\foo\\', name: 'foo',
                cVisualattributes: 'FAS', cComment: 'trial:STUDY_ID')
        addI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'var',
                cVisualattributes: 'FH', cComment: 'trial:STUDY_ID')
        addI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto',
                cComment: 'trial:STUDY_ID')
        addI2b2(level: 2, fullName: '\\foo\\xpto\\bar\\', name: 'bar',
                cComment: 'trial:STUDY_ID')

        addI2b2(level: 3, fullName: '\\foo\\xpto\\bar\\jar\\', name: 'jar')
        addI2b2(level: 3, fullName: '\\foo\\xpto\\bar\\hd\\', name: 'hd',
                cVisualattributes: 'LAH')

        addTableAccess(level: 0, fullName: '\\shared\\', name: 'shared',
                tableCode: 'shared', tableName: 'i2b2')
        addI2b2(level: 0, fullName: '\\shared\\', name: 'shared',
                cVisualattributes: 'FA')
        addI2b2(level: 1, fullName: '\\shared\\property\\', name: 'property',
                cVisualattributes: 'LA')
        sessionFactory.currentSession.flush()
    }

    void testGetVisualAttributes() {
        setupData()
        def terms = I2b2.withCriteria { eq 'level', 1 }

        //currently testing against postgres database,
        // which has already a matching row there
        expect:
        terms hasSize(greaterThanOrEqualTo(2))
        // terms hasSize(2)

        terms hasItem(allOf(
                hasProperty('name', equalTo('var')),
                hasProperty('visualAttributes', containsInAnyOrder(
                        OntologyTerm.VisualAttributes.FOLDER,
                        OntologyTerm.VisualAttributes.HIDDEN
                ))
        ))
    }

    void testGetTableCode() {
        setupData()
        I2b2 bar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\') }

        expect:
        bar.conceptKey == new ConceptKey('i2b2 table code', '\\foo\\xpto\\bar\\')
    }

    void testGetChildren() {
        setupData()
        when:
        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto\\') }
        xpto.setTableCode('i2b2 table code OOOO')
        then:
        xpto is(notNullValue())

        when:
        def children = xpto.children
        then:
        children.size == 1
        children[0].fullName
        children[0].fullName == '\\foo\\xpto\\bar\\'
        children[0].conceptKey
        children[0].conceptKey == new ConceptKey('\\\\i2b2 table code OOOO\\foo\\xpto\\bar\\')
    }

    void testGetAllDescendants() {
        setupData()
        when:
        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto\\') }
        then:
        xpto is(notNullValue())

        when:
        def children = xpto.allDescendants
        then:
        children allOf(
                hasSize(3),
                contains(
                        hasProperty('name', equalTo('bar')),
                        hasProperty('name', equalTo('hd')),
                        hasProperty('name', equalTo('jar')),
                )
        )
    }

    void testGetHDForAllDescendants() {
        setupData()
        when:
        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto\\') }
        then:
        xpto

        when:
        def children = xpto.HDforAllDescendants
        then:
        children.size() == 1
        children.first().name == 'hd'
    }

    void testGetStudy() {
        setupData()
        I2b2 bar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\') }
        I2b2 foo = I2b2.find { eq('fullName', '\\foo\\') }

        Study study = bar.study
        expect:
        study allOf(
                hasProperty('id', is('STUDY_ID')),
                hasProperty('ontologyTerm', is(foo)),
        )
    }

    void testGetStudyNull() {
        setupData()
        I2b2 sharedProperty = I2b2.find { eq('fullName', '\\shared\\property\\') }
        expect:
        sharedProperty.study == null
    }

    void testGetPatients() {
        setupData()
        ConceptTestData.addTableAccess(level: 0, fullName: '\\test\\', name: 'test',
                tableCode: 'i2b2 main', tableName: 'i2b2')

        def concepts = ConceptTestData.createMultipleI2B2(3)
        def patients = I2b2Data.createTestPatients(5, -100, 'SAMPLE TRIAL')
        def observations = createObservations(concepts, patients)

        HighDimTestData.save concepts
        HighDimTestData.save patients
        HighDimTestData.save observations
        HighDimTestData.save ConceptTestData.createConceptDimensions(concepts)
        sessionFactory.currentSession.flush()

        when:
        def result = concepts[0].getPatients()
        then:
        result containsInAnyOrder(
                patients[0],
                patients[1],
                patients[2]
        )

        when:
        def result2 = concepts[1].getPatients()
        then:
        result2 containsInAnyOrder(
                patients[2],
                patients[3]
        )

        when:
        def result3 = concepts[2].getPatients()
        then:
        result3 containsInAnyOrder(
                //empty
        )

    }

    void testSynonymIsTransient() {
        setupData()
        PersistentEntity entity =
                Holders.grailsApplication.mappingContext.getPersistentEntity('org.transmartproject.db.ontology.I2b2')

        expect:
        !entity.persistentPropertyNames.contains('synonym')
    }

    void testSynonym() {
        setupData()
        I2b2 i2b2 = I2b2.find { eq('fullName', '\\foo\\xpto\\bar\\jar\\') }

        expect:
        !i2b2.synonym

        when:
        i2b2.cSynonymCd = 'Y'
        then:
        i2b2.synonym
    }

    def createObservations(List<I2b2> concepts, List<Patient> patients) {
        List result = []
        org.transmartproject.db.i2b2data.Study dummyStudy = StudyTestData.createDefaultTabularStudy()
        TrialVisit dummyTrialVisit = ClinicalTestData.createTrialVisit("fake", 1, null, dummyStudy)
        //concept 0 with patients 0, 1 and 2
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[0], 1, 2, dummyTrialVisit)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[1], 1, 2, dummyTrialVisit)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[2], 1, 2, dummyTrialVisit)
        //concept 1 with patients 2 and 3
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[2], 1, 2, dummyTrialVisit)
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[3], 1, 2, dummyTrialVisit)
        result
    }

}
