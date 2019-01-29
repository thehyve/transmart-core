package org.transmartproject.core.multidimquery.hypercube

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Contains information about a field of a dimension
 */
@CompileStatic
@Canonical
class Field {
    String name
    Type type
}
