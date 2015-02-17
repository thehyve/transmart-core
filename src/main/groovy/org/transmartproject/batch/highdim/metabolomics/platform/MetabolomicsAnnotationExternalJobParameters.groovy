package org.transmartproject.batch.highdim.metabolomics.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobParameters

/**
 * External parameters for metabolomics annotations.
 */
class MetabolomicsAnnotationExternalJobParameters extends AbstractPlatformJobParameters {
    final String markerType = 'METABOLOMICS'
    final Class jobPath = MetabolomicsPlatformJobConfiguration
}
