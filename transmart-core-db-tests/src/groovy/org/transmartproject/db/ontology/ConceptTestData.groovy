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
        i2b2List << createI2b2Concept(code: 1, level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY1', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 2, level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY1', cVisualattributes: 'LA')
        i2b2List << createI2b2Concept(code: 3, level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY2', cVisualattributes: 'FA')
        i2b2List << createI2b2Concept(code: 4, level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY2', cVisualattributes: 'LA')
        // used only in AccessLevelTestData
        i2b2List << createI2b2Concept(code: 5, level: 1, fullName: '\\foo\\study3\\',         name: 'study3', cComment: 'trial:STUDY3', cVisualAttributes: 'FA')
        // useful to test rest-api
        i2b2List << createI2b2Concept(code: 6, level: 2, fullName: '\\foo\\study2\\long path\\',
                                      name: 'study2', cComment: 'trial:STUDY2', cVisualAttributes: 'FA')
        i2b2List << createI2b2Concept(code: 7, level: 3, fullName: '\\foo\\study2\\long path\\with%some$characters_\\',
                                      name: 'study2', cComment: 'trial:STUDY2', cVisualAttributes: 'LA')

        def conceptDimensions = createConceptDimensions(i2b2List)

        new ConceptTestData(tableAccesses: tableAccesses, i2b2List: i2b2List, conceptDimensions: conceptDimensions)
    }

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

    static def checkValidForOntologyQueries(I2b2 input) {
        def missing = TestDataHelper.getMissingValueFields(input, ontologyQueryRequiredFields)
        if (missing.size() > 0) {
            throw new IllegalArgumentException("Some I2b2 instances miss fields required for ontology queries: $missing")
        }
    }

}
