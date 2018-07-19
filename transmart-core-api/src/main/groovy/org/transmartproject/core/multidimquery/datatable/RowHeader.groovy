package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class RowHeader {
    String dimension
    Object element
    Object key
}
