package org.transmartproject.batch.clinical.facts

import groovy.transform.ToString

/**
 * Represents a row of a clinical data file.
 */
@ToString(includes = ['filename', 'index'])
class ClinicalDataRow {
    String filename
    int index
    List<String> values

    String getAt(int index) {
        values[index]?.trim()
    }
}
