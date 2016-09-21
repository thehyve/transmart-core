package org.transmartproject.batch.highdim.mirna.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for miRNA platform loading.
 */
final class MirnaAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'MIRNA_QPCR'
    final Class jobPath = MirnaPlatformJobConfig
}
