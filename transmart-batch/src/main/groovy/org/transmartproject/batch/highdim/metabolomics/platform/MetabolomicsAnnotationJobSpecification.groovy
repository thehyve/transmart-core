package org.transmartproject.batch.highdim.metabolomics.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for metabolomics annotations.
 */
final class MetabolomicsAnnotationJobSpecification
        extends AbstractPlatformJobSpecification {
    final String markerType = 'METABOLOMICS'
    final Class jobPath = MetabolomicsPlatformJobConfig
}
