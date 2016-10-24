package org.transmartproject.batch.highdim.rnaseq.transcript.platform

import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification

/**
 * Job specification for RNASeq platform (trascript level) loading.
 */
final class RnaSeqTranscriptAnnotationJobSpecification extends AbstractPlatformJobSpecification {
    final String markerType = 'RNASEQ_TRANSCRIPT'
    final Class jobPath = RnaSeqTranscriptAnnotationJobConfig
}
