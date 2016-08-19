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

import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.user.AccessLevelTestData

import static org.transmartproject.db.TestDataHelper.save
import static org.transmartproject.db.dataquery.clinical.ClinicalTestData.*
import static org.transmartproject.db.i2b2data.I2b2Data.createTestPatients
import static org.transmartproject.db.ontology.ConceptTestData.createConceptDimensions
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2Concept

class AcrossTrialsTestData {

    public static final String MODIFIER_MALE = 'SNOMED:F-03CE6'
    public static final String MODIFIER_FEMALE = 'SNOMED:F-03CE5'
    public static final String MODIFIER_AGE_AT_DIAGNOSIS = 'SNOMED:F-08104'

    ConceptTestData conceptTestData
    List<PatientDimension> patients
    List<ObservationFact> facts
    AccessLevelTestData accessLevelTestData
    List<ModifierDimensionCoreDb> modifierDimensions
    List<ModifierMetadataCoreDb> modifierMetadatas

    static AcrossTrialsTestData createDefault() {
        def list = []
        list << createModifier(path: '\\Demographics\\',
                code: 'CDEMO', nodeType: 'F')
        list << createModifier(path: '\\Demographics\\Age at Diagnosis\\',
                code: MODIFIER_AGE_AT_DIAGNOSIS, nodeType: 'L', valueType: 'N', unit: 'year')
        list << createModifier(path: '\\Demographics\\Sex\\',
                code: 'SNOMED:F-03D86', nodeType: 'F')
        list << createModifier(path: '\\Demographics\\Sex\\Female\\',
                code: MODIFIER_FEMALE, nodeType: 'L')
        list << createModifier(path: '\\Demographics\\Sex\\Male\\',
                code: MODIFIER_MALE, nodeType: 'L')

        def result = new AcrossTrialsTestData()
        result.modifierDimensions = list*.get(0)
        result.modifierMetadatas = list*.get(1)

        def tableAccess = ConceptTestData.createTableAccess(
                level:     0,
                fullName:  '\\foo\\',
                name:      'foo',
                tableCode: 'i2b2 main',
                tableName: 'i2b2')

        int c = 1;
        def i2b2List = [
                createI2b2Concept(code: c++, level: 1, fullName: '\\foo\\study1\\', name: 'study1',
                        cComment: 'trial:STUDY_ID_1', cVisualattributes: 'FA'),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study1\\age at diagnosis\\',
                        name: 'age at diagnosis', cComment: 'trial:STUDY_ID_1', cVisualattributes: 'LA',
                        metadataxml: ConceptTestData.numericXml),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study1\\male\\', name: 'male',
                        cComment: 'trial:STUDY_ID_1', cVisualattributes: 'LA'),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study1\\female\\', name: 'female',
                        cComment: 'trial:STUDY_ID_1', cVisualattributes: 'LA'),
                createI2b2Concept(code: c++, level: 1, fullName: '\\foo\\study2\\', name: 'study2',
                        cComment: 'trial:STUDY_ID_2', cVisualattributes: 'FA'),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study2\\age at diagnosis\\',
                        name: 'age at diagnosis', cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA',
                        metadataxml: ConceptTestData.numericXml),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study2\\male\\', name: 'male',
                        cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA'),
                createI2b2Concept(code: c++, level: 2, fullName: '\\foo\\study2\\female\\', name: 'female',
                        cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA'),
        ]

        result.conceptTestData = new ConceptTestData(tableAccesses: [tableAccess],
                i2b2List: i2b2List, conceptDimensions: createConceptDimensions(i2b2List))

        def patientsStudy1 = createTestPatients(2, -400L, 'STUDY_ID_1')
        def patientsStudy2 = createTestPatients(2, -500L, 'STUDY_ID_2')

        def conceptDimensionFor = { String fullName ->
            result.conceptTestData.conceptDimensions.find {
                fullName == fullName
            }
        }

        List<ObservationFact> observations = [
                createDiagonalCategoricalFacts(2, i2b2List.findAll {
                    it.fullName =~ /\\foo\\study1\\(fe)?male/
                }, patientsStudy1),
                createDiagonalCategoricalFacts(2, i2b2List.findAll {
                    it.fullName =~ /\\foo\\study2\\(fe)?male/
                }, patientsStudy2),
                createObservationFact(conceptDimensionFor('\\foo\\study1\\age at diagnosis\\'),
                        patientsStudy1[0], DUMMY_ENCOUNTER_ID, 1100),
                createObservationFact(conceptDimensionFor('\\foo\\study1\\age at diagnosis\\'),
                        patientsStudy1[1], DUMMY_ENCOUNTER_ID, 2101),
                createObservationFact(conceptDimensionFor('\\foo\\study2\\age at diagnosis\\'),
                        patientsStudy2[0], DUMMY_ENCOUNTER_ID, 1200),
                createObservationFact(conceptDimensionFor('\\foo\\study2\\age at diagnosis\\'),
                        patientsStudy2[1], DUMMY_ENCOUNTER_ID, 2201),
        ].flatten()
        observations[0].modifierCd = MODIFIER_MALE
        observations[1].modifierCd = MODIFIER_FEMALE
        observations[2].modifierCd = MODIFIER_MALE
        observations[3].modifierCd = MODIFIER_FEMALE
        observations[4..7]*.modifierCd = MODIFIER_AGE_AT_DIAGNOSIS

        result.patients = patientsStudy1 + patientsStudy2
        result.facts = observations

        result.accessLevelTestData = AccessLevelTestData.
                createWithAlternativeConceptData(result.conceptTestData)

        result
    }

    /*
     * Properties:
     * - path
     * - code
     * - nodeType (L/F)
     * - name (optional, calculated)
     * - level (optional, calculated)
     * - studyId (optional)
     * - valueType (optional, N/T, calculated)
     * - unit (optional)
     */
    static List createModifier(Map<String, Object> properties) {
        if (['path', 'code', 'nodeType'].
                collect { properties."$it" == null }.any()) {
            throw new IllegalArgumentException("Missing required property")
        }
        if (!properties.name) {
            properties.name = new ConceptFullName(properties.path)[-1]
        }
        if (!properties.level) {
            properties.level = new ConceptFullName(properties.path).length - 1
        }
        if (!properties.valueType) {
            properties.valueType = 'T'
        }
        properties.visitInd = 'N'

        def dimension = new ModifierDimensionCoreDb(properties)
        def metadata = new ModifierMetadataCoreDb(properties)

        [dimension, metadata]
    }


    void saveAll() {
        save modifierDimensions
        save modifierMetadatas
        conceptTestData.saveAll()
        save patients
        save facts
        accessLevelTestData.saveAll()
    }
}
