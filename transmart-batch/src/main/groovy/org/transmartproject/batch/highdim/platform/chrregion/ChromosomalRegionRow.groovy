package org.transmartproject.batch.highdim.platform.chrregion

import groovy.transform.Canonical
import org.transmartproject.batch.highdim.datastd.ChromosomalRegionSupport
import org.transmartproject.batch.highdim.datastd.PlatformOrganismSupport

/**
 * Represents a line on the chromosomal region file
 */
@Canonical
class ChromosomalRegionRow implements ChromosomalRegionSupport, PlatformOrganismSupport {
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
