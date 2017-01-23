package org.transmartproject.batch.highdim.proteomics.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for proteomics platform loading.
 */
final class ProteomicsAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'PROTEOMICS'
    final Class jobPath = ProteomicsPlatformJobConfig
}
