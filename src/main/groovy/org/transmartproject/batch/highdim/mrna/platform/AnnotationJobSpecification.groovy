package org.transmartproject.batch.highdim.mrna.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for mRNA platform loading. Use {@link MrnaAnnotationJobSpecification} instead.
 */
@Deprecated
final class AnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'Gene Expression'
    final Class jobPath = MrnaPlatformJobConfig
}
