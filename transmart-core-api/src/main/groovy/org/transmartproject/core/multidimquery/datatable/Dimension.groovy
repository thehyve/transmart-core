package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Dimension {
    String name
    Map<Object, Object> elements
}
