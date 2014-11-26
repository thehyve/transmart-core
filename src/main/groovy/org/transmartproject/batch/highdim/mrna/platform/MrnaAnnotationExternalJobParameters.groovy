package org.transmartproject.batch.highdim.mrna.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobParameters

/**
 *
 */
class MrnaAnnotationExternalJobParameters extends AbstractPlatformJobParameters {
    final String markerType = 'Gene Expression'
    final Class jobPath = MrnaPlatformJobConfiguration
}
