package org.transmartproject.batch.highdim.cnv.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionJobConfig

/**
 * Job specification for CNV platform loading.
 */
final class CnvAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'Chromosomal'
    final Class jobPath = ChromosomalRegionJobConfig
}
