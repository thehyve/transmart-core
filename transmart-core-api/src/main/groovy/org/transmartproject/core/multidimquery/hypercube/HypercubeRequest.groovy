package org.transmartproject.core.multidimquery.hypercube

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint

@CompileStatic
@Canonical
class HypercubeRequest {
    String type
    Constraint constraint
}
