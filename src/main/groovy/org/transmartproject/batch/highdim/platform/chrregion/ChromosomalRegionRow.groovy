package org.transmartproject.batch.highdim.platform.chrregion

import groovy.transform.Canonical

/**
 * Represents a line on the chromosomal region file
 */
@Canonical
class ChromosomalRegionRow {

    String gplId
    String chromosome
    Long startBp
    Long endBp
    Long numProbes
    String regionName
    String cytoband
    String geneSymbol
    Long geneId
    String organism

}
