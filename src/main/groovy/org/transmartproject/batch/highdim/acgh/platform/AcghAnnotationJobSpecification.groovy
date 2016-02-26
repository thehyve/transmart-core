package org.transmartproject.batch.highdim.acgh.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionJobConfig

/**
 * Job specification for ACGH platform loading.
 */
final class AcghAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'Chromosomal'
    final Class jobPath = ChromosomalRegionJobConfig
}
