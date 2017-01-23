package org.transmartproject.batch.highdim.mrna.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface

/**
 * Job specification for mRNA platform loading.
 */
final class MrnaAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    public final static String HAS_HEADER = 'HAS_HEADER'

    final String markerType = 'Gene Expression'
    final Class jobPath = MrnaPlatformJobConfig

    Set<String> supportedParameters = super.supportedParameters + HAS_HEADER

    void munge(ExternalJobParametersInternalInterface ejp) {
        super.munge(ejp)
        ejp[HAS_HEADER] = 'Y'
    }

}
