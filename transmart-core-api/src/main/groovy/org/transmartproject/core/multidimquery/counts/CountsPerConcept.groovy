package org.transmartproject.core.multidimquery.counts

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CountsPerConcept {
    Map<String, Counts> countsPerConcept
}
