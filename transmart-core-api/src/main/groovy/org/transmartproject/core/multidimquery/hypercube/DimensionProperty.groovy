package org.transmartproject.core.multidimquery.hypercube

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * @see Property
 */
//TODO
@CompileStatic
@Canonical
class DimensionProperty {
    String name
    String type
    Set<DimensionProperty> fields
    Boolean inline
}
