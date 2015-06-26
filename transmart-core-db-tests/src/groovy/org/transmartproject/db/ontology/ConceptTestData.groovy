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

import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension

import static org.transmartproject.db.TestDataHelper.save

class ConceptTestData {

    List<TableAccess> tableAccesses
    List<I2b2> i2b2List
    List<I2b2Tag> i2b2TagsList
    List<ConceptDimension> conceptDimensions

    static ConceptTestData createDefault() {
        def tableAccesses = []
        tableAccesses << createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo', tableCode: 'i2b2 main', tableName: 'i2b2')

        def i2b2List = []
        i2b2List << createI2b2Concept(code: -1, level: 0, fullName: '\\foo\\', name: 'foo', cVisualattributes: 'CA')
        i2b2List << createI2b2Concept(code: 1, level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY_ID_1', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 2, level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY_ID_1', cVisualattributes: 'LAH', metadataxml: numericXml)
        i2b2List << createI2b2Concept(code: 3, level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY_ID_2', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 4, level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LAH', metadataxml: numericXml)
        // used only in AccessLevelTestData
        i2b2List << createI2b2Concept(code: 5, level: 1, fullName: '\\foo\\study3\\',         name: 'study3', cComment: 'trial:STUDY_ID_3', cVisualattributes: 'FA')
        // useful to test rest-api
        i2b2List << createI2b2Concept(code: 6, level: 2, fullName: '\\foo\\study2\\long path\\',
                                      name: 'long path', cComment: 'trial:STUDY_ID_2', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 7, level: 3, fullName: '\\foo\\study2\\long path\\with%some$characters_\\',
                                      name: 'with%some$characters_', cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA',
                                      metadataxml: numericXml)
        //categorical node
        i2b2List << createI2b2Concept(code: 8, level: 2, fullName: '\\foo\\study2\\sex\\',          name: 'sex',
                                      cComment: 'trial:STUDY_ID_2', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 9, level: 3, fullName: '\\foo\\study2\\sex\\male\\',    name: 'male',
                                      cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA')
        i2b2List << createI2b2Concept(code: 10, level: 3, fullName: '\\foo\\study2\\sex\\female\\', name: 'female',
                                      cComment: 'trial:STUDY_ID_2', cVisualattributes: 'LA')

        def conceptDimensions = createConceptDimensions(i2b2List)

        def i2b2Tags = createI2b2Tags(i2b2List, 2)

        new ConceptTestData(
                tableAccesses: tableAccesses,
                i2b2List: i2b2List,
                conceptDimensions: conceptDimensions,
                i2b2TagsList: i2b2Tags)
    }

    static numericXml = '''<ValueMetadata>
  <Oktousevalues>Y</Oktousevalues>
</ValueMetadata>
'''

    /**
     * map with common default values for creating a concept
     */
    static Map conceptDefaultValues = {
        [
                factTableColumn: 'CONCEPT_CD',
                dimensionTableName: 'CONCEPT_DIMENSION',
                columnName: 'CONCEPT_PATH',
                operator: 'LIKE',
                columnDataType: 'T',
        ]
    }()

    static Set<String> ontologyQueryRequiredFields = {
        def result = []
        result.add 'dimensionCode'
        result.addAll getConceptDefaultValues().keySet().asList()
        result
    }()

    void saveAll() {
        save tableAccesses
        save i2b2List
        save i2b2TagsList
        save conceptDimensions
    }

    private static Map i2b2xBase = [
            factTableColumn      :   '',
            dimensionTableName   :   '',
            columnName           :   '',
            columnDataType       :   '',
            operator             :   '',
            dimensionCode        :   '',
            mAppliedPath         :   '',
            updateDate           :   new Date()
    ]

    static I2b2 createI2b2(Map properties) {
        new I2b2([*:i2b2xBase, *:properties])
    }

    static I2b2Secure createI2b2Secure(Map properties) {
        new I2b2Secure([*:i2b2xBase, *:properties])
    }

    /**
     * Creates an I2b2 concept with extra fields set so some observation/patient related queries can work
     * @param properties
     * @return
     */
    static I2b2 createI2b2Concept(Map properties) {

        assert properties.get('fullName')
        assert properties.get('code')

        def extraProps = [
                dimensionCode : properties.get('fullName') //needed for ontology queries
        ]
        //field values are set in layers:
        //1 - getConceptDefaultValues(): typical values for concepts
        //2 - extraProps: derived or unique per object
        //3 - properties: values given by the client (these will override all others)
        //4 - completes any remaining mandatory fields with a dummy value
        def o = new I2b2([*:getConceptDefaultValues(), *:extraProps, *:properties])
        TestDataHelper.completeObject(I2b2, o)
        //we need to make sure the i2b2 instance is valid for the ontology queries
        checkValidForOntologyQueries(o)
        o
    }

    static addI2b2(Map properties) {
        assert createI2b2(properties).save() != null
    }

    static TableAccess createTableAccess(Map properties) {
        def base = [
                level                :   0,
                factTableColumn      :   '',
                dimensionTableName   :   '',
                columnName           :   '',
                columnDataType       :   '',
                operator             :   '',
                dimensionCode        :   '',
        ]

        new TableAccess([*:base, *:properties])
    }

    static addTableAccess(Map properties) {
        assert createTableAccess(properties).save() != null
    }

    static List<I2b2> createMultipleI2B2(int count, String basePath = "\\test", String codePrefix = "test", int level = 1) {
        (1..count).collect { int i ->
            def name = "concept$i"
            def fullName = "$basePath\\$name\\"
            def props = [
                name: name,
                fullName: fullName,
                code: "$codePrefix$i",
                level: level,
                dimensionCode: fullName
            ]

            I2b2 o = new I2b2([*: getConceptDefaultValues(), *:props])
            TestDataHelper.completeObject(I2b2, o) //completes the object with any missing values for mandatory fields
            o
        }
    }

    /**
     * @param list
     * @return ConceptDimension list for the given I2b2 list
     */
    static List<ConceptDimension> createConceptDimensions(List<I2b2> list) {
        list.collect {
            assert it.code != null
            new ConceptDimension(conceptPath: it.fullName, conceptCode: it.code)
        }
    }

    static List<I2b2Tag> createI2b2Tags(List<I2b2> list, int number = 2) {
        list.collectMany { I2b2 i2b2 ->
            (1..number).collect { iteration ->
                new I2b2Tag(
                        ontologyTermFullName: i2b2.fullName,
                        name: "${i2b2.code} name ${iteration}",
                        description: "${i2b2.code} description ${iteration}",
                        //for reverse order
                        position: number - iteration + 1
                )
            }
        }
    }

    static def checkValidForOntologyQueries(I2b2 input) {
        def missing = TestDataHelper.getMissingValueFields(input, ontologyQueryRequiredFields)
        if (missing.size() > 0) {
            throw new IllegalArgumentException("Some I2b2 instances miss fields required for ontology queries: $missing")
        }
    }

    /**
     * Adds a leaf concept to this data, along with its folder and root concepts.
     *
     * @param root name of the root concept to be created
     * @param study name of the folder concept to be created
     * @param concept name of the concep to be created
     * @param code concept code
     * @return new concept
     */
    I2b2 addLeafConcept(String root = 'base',
                        String study = 'folder',
                        String concept = 'leaf',
                        String code = 'mycode') {

        initListsIfNull()

        tableAccesses << createTableAccess(fullName: "\\$root\\", name: root, tableCode: 'i2b2 main', tableName: 'i2b2')
        i2b2List << createI2b2Concept(level: 1, fullName: "\\$root\\$study\\", name: study,
                code: study, cVisualattributes: 'FA')
        I2b2 result = createI2b2Concept(level: 2, fullName: "\\$root\\$study\\$concept\\", name: concept,
                code: code,  cVisualattributes: 'LA')

        i2b2List << result

        conceptDimensions.addAll(createConceptDimensions([result]))

        result
    }

    private void initListsIfNull() {
        this.tableAccesses = this.tableAccesses ?: []
        this.i2b2List = this.i2b2List ?: []
        this.conceptDimensions = this.conceptDimensions ?: []
    }


}
