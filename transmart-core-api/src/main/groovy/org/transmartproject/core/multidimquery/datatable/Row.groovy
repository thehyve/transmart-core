package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Row {
    List<RowHeader> rowHeaders
    List<Object> cells
}
