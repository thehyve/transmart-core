package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class ColumnHeader {
    String dimension
    List<Object> elements
    List<Object> keys
}
