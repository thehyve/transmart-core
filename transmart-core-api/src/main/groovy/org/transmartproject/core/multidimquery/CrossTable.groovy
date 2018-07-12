package org.transmartproject.core.multidimquery

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * A cross table is a list of rows. Each row contains list of cells with a subject count.
 */
@CompileStatic
@Canonical
class CrossTable {

    /**
     * @return a list of rows with values
     */
    List<List<Long>> rows

}
