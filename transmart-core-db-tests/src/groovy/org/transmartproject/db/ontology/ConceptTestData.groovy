package org.transmartproject.db.ontology

import org.transmartproject.db.dataquery.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension

class ConceptTestData {

    private static Set<String> ontologyQueryRequiredFields = computeOntologyQueryRequiredFields()

    static I2b2 createI2b2(Map properties) {
        def base = [
                factTableColumn      :   '',
                dimensionTableName   :   '',
                columnName           :   '',
                columnDataType       :   '',
                operator             :   '',
                dimensionCode        :   '',
                mAppliedPath         :   '',
                updateDate           :   new Date()
        ]

        new I2b2([*:base, *:properties])
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

            createConcept(props)
        }
    }

    /**
     * @param list
     * @return ConceptDimension list for the given I2b2 list
     */
    static List<ConceptDimension> createConceptDimensions(List<I2b2> list) {
        list.collect {
            new ConceptDimension(conceptPath: it.fullName, conceptCode: it.code)
        }
    }

    /**
     * @return map with common default values for creating an i2b2 concept
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

    /**
     * Creates an I2b2 concept with extra fields set so some observation/patient related queries can work
     * @param properties
     * @return
     */
    static I2b2 createConcept(Map properties) {
        def extraProps = [
                code:           getNextConceptCode(),
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

    static String getNextConceptCode() {
        def id = TestDataHelper.getNextId(I2b2)
        "dummy$id"
    }

    static Set<String> computeOntologyQueryRequiredFields() {
        def result = []
        result.add 'dimensionCode'
        result.addAll getConceptDefaultValues().keySet()
        result
    }

    static def checkValidForOntologyQueries(I2b2 input) {
        def missing = TestDataHelper.getMissingValueFields(input, ontologyQueryRequiredFields)
        if (missing.size() > 0) {
            throw new IllegalArgumentException("Some I2b2 instances miss fields required for ontology queries: $missing")
        }
    }

}
