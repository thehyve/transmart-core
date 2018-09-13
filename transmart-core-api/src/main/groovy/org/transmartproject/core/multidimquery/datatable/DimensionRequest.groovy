package org.transmartproject.core.multidimquery.datatable

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class DimensionRequest {
    String name
    Map<String, Object> elements
}
