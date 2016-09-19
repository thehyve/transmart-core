package org.transmartproject.batch.highdim.proteomics.platform

import groovy.transform.Canonical
import org.transmartproject.batch.highdim.datastd.ChromosomalRegionSupport
import org.transmartproject.batch.highdim.datastd.PlatformOrganismSupport

/**
 * Represents a line on the proteomics annotations file
 */
@Canonical
class ProteomicsAnnotationRow implements ChromosomalRegionSupport, PlatformOrganismSupport {

    String gplId
    String probesetId
    String uniprotId
    String uniprotName
    String organism

    String chromosome
    Long startBp
    Long endBp

}
