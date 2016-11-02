package org.transmartproject.batch.highdim.rnaseq.transcript.platform

import groovy.transform.Canonical

/**
 * Represents a line on the transcript file
 */
@Canonical
class RnaSeqTranscriptAnnotationRow {
    String gplId
    String refId
    String chromosome
    Long startBp
    Long endBp
    String transcript
}
