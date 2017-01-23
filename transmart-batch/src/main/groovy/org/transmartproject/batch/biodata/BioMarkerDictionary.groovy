package org.transmartproject.batch.biodata

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component

/**
 * Access bio markers dictionary.
 */
@Component
@JobScope
class BioMarkerDictionary {

    private Map<String, String> uniprotIdToUniprotName

    void setUniprotIdToUniprotName(Map<String, String> uniprotIdToUniprotName) {
        this.uniprotIdToUniprotName = uniprotIdToUniprotName
    }

    String getUniprotNameByUniporotId(String uniprotId) {
        uniprotIdToUniprotName[uniprotId]
    }
}
