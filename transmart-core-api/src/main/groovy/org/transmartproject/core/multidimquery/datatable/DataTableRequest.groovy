package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint

@CompileStatic
@Canonical
class DataTableRequest {
    String type
    Constraint constraint
    List<String> rowDimensions
    List<String> columnDimensions
    int limit
}
