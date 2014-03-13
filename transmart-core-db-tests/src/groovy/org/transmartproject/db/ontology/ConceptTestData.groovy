package org.transmartproject.db.ontology

import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension

import static org.transmartproject.db.TestDataHelper.save

class ConceptTestData {

    List<TableAccess> tableAccesses
    List<I2b2> i2b2List
    List<ConceptDimension> conceptDimensions

    static ConceptTestData createDefault() {
        def tableAccesses = []
        tableAccesses << createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo', tableCode: 'i2b2 main', tableName: 'i2b2')

        def i2b2List = []
        i2b2List << createI2b2(code: 1, level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY1', cVisualattributes: 'FA')
        i2b2List << createI2b2(code: 2, level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY1', cVisualattributes: 'LA')
        i2b2List << createI2b2(code: 3, level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY2', cVisualattributes: 'FA')
        i2b2List << createI2b2(code: 4, level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY2', cVisualattributes: 'LA')
        // used only in AccessLevelTestData
        i2b2List << createI2b2(code: 5, level: 1, fullName: '\\foo\\study3\\',         name: 'study3', cComment: 'trial:STUDY3', cVisualAttributes: 'FA')

        def conceptDimensions = createConceptDimensions(i2b2List)

        new ConceptTestData(tableAccesses: tableAccesses, i2b2List: i2b2List, conceptDimensions: conceptDimensions)
    }

    private static Set<String> getOntologyQueryRequiredFields() {
        def result = []
        result.add 'dimensionCode'
        result.addAll getConceptDefaultValues().keySet().asList()
        result
    }

    void saveAll() {
        save tableAccesses
        save i2b2List
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

    /**
     * @return map with common default values for creating a concept
     */
    static Map getConceptDefaultValues() {
        Map result = [
            factTableColumn: 'CONCEPT_CD',
            dimensionTableName: 'CONCEPT_DIMENSION',
            columnName: 'CONCEPT_PATH',
            operator: 'LIKE',
            columnDataType: 'T',
        ]

        result
    }

}
