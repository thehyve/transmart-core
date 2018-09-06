package org.transmartproject.core.multidimquery.counts

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CountsPerStudy {
    Map<String, Counts> countsPerStudy
}
