package org.transmartproject.batch.highdim.metabolomics.platform

import groovy.transform.ToString

/**
 * Represents a row in the metabolomics annotation file.
 */
@ToString(includeNames = true, includePackage = false)
class MetabolomicsAnnotationRow {

    String biochemical

    String superPathway

    String subPathway

    String hmdbId

    void setHmdbId(String hmdbId) {
        this.hmdbId = hmdbId ?: null // '' to null
    }
}
