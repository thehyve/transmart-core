package org.transmartproject.batch.highdim.proteomics.platform

import groovy.transform.Canonical

/**
 * Represents a line on the proteomics annotations file
 */
@Canonical
class ProteomicsAnnotationRow {

    String gplId
    String probesetId
    String uniprotId
    String organism

    String chromosome
    Long startBp
    Long endBp

}
