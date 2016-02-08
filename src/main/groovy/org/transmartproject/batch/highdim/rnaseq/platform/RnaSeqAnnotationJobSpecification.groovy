package org.transmartproject.batch.highdim.rnaseq.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionJobConfiguration

/**
 * Job specification for RNASeq platform loading.
 */
final class RnaSeqAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'RNASEQ_RCNT'
    final Class jobPath = ChromosomalRegionJobConfiguration
}
