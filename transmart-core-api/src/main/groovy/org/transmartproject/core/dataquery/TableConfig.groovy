package org.transmartproject.core.dataquery

import groovy.transform.CompileStatic

import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

/**
 * tableConfig: { //additional config, required for the data table export
 *   rowDimensions: [ "<list of dimension names>" ] //specifies the row dimensions of the data table
 *   columnDimensions: [ "<list of dimension names>" ] //specifies the column dimensions of the data table
 *   rowSort: [ "<list of sort specifications>" ] // Json list of sort specifications for the row dimensions
 *   columnSort: [ "<list of sort specifications>" ] // Json list of sort specifications for the column dimensions
 * }
 */
@CompileStatic
class TableConfig {

    // Specifies the row dimensions of the data table
    List<String> rowDimensions
    // Specifies the column dimensions of the data table
    List<String> columnDimensions
    // List of sort specifications for the row dimensions
    List<SortSpecification> rowSort
    // List of sort specifications for the column dimensions
    List<SortSpecification> columnSort

    @Min(0L)
    Integer offset

    @NotNull
    @Min(0L)
    Integer limit

}
