package org.transmartproject.core.multidimquery.counts

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CountsPerStudyAndConcept {
    Map<String, Map<String, Counts>> countsPerStudy
}
