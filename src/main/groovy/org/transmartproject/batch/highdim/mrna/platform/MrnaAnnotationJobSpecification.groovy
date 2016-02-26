package org.transmartproject.batch.highdim.mrna.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for mRNA platform loading.
 */
final class MrnaAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'Gene Expression'
    final Class jobPath = MrnaPlatformJobConfig
}
