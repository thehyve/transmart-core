package org.transmartproject.core.ontology

import groovy.transform.CompileStatic

@CompileStatic
class MissingValues {
    BigDecimal lower
    BigDecimal upper
    List<Object> values = []
}
