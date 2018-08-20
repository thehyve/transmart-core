package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.SortSpecification

@CompileStatic
@Canonical
class DataTable {
    List<ColumnHeader> columnHeaders
    List<DimensionRequest> rowDimensions
    List<DimensionRequest> columnDimensions
    Integer rowCount
    List<Row> rows
    Integer offset
    List<SortSpecification> sort
}
