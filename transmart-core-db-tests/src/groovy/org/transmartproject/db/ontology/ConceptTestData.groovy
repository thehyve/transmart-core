package org.transmartproject.db.ontology

import org.transmartproject.db.dataquery.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension

class ConceptTestData {

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
