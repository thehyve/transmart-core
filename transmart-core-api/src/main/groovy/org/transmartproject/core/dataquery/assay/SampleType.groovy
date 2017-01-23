package org.transmartproject.core.dataquery.assay

import groovy.transform.Canonical

@Canonical
class SampleType {

    /**
     * The sample code used for querying purposes. Typically a numerical
     * value.
     */
    String code

    /**
     * A label used for displaying purposes.
     */
    String label

}
