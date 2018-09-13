package org.transmartproject.db.multidimquery

import spock.lang.Specification


class AbstractDataTableSpec extends Specification {


    def "test dataTable row implementation mapping"() {
        setup: "I create DataTableRowImpl"
        List mockElements = ["elem1", "elem2", "elem3"]
        List mockKeys = ["zeroKey", "firstKey", "secondKey"]
        int offset = 0
        AbstractDataTable.DataTableRowImpl dataTableRow = new AbstractDataTable.DataTableRowImpl(offset, mockElements, mockKeys)

        when: "I try to modify row elements"
        dataTableRow.elements.add("elem3")

        then: "Exception is thrown"
        thrown(UnsupportedOperationException)
        dataTableRow.elements == mockElements

        when: "I try to modify row keys"
        dataTableRow.keys.add("thirdKey")

        then: "Exception is thrown"
        thrown(UnsupportedOperationException)
        dataTableRow.keys == mockKeys
    }

}
