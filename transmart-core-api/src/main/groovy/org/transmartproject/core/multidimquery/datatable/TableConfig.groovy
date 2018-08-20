package org.transmartproject.core.multidimquery.datatable

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.SortSpecification

/**
 * Configuration required for the data table export
 */
@CompileStatic
class TableConfig {

    /**
     * Specifies the row dimensions of the data table
     */
    List<String> rowDimensions

    /**
     * Specifies the column dimensions of the data table
     */
    List<String> columnDimensions

    /**
     * List of sort specifications for the row dimensions
     */
    List<SortSpecification> rowSort

    /**
     * List of sort specifications for the column dimensions
     */
    List<SortSpecification> columnSort

}
