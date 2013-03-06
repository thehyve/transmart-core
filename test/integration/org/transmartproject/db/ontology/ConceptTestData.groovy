package org.transmartproject.db.ontology

class ConceptTestData {

    static addI2b2(Map properties) {
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

        def obj = new I2b2([*:base, *:properties])
        assert obj.save() != null
    }

    static addTableAccess(Map properties) {
        def base = [
                factTableColumn      :   '',
                dimensionTableName   :   '',
                columnName           :   '',
                columnDataType       :   '',
                operator             :   '',
                dimensionCode        :   '',
        ]

        def obj = new TableAccess([*:base, *:properties])
        assert obj.save() != null
    }

}
