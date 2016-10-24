package org.transmartproject.batch.highdim.platform.chrregion.transcript

import groovy.transform.Canonical
import org.transmartproject.batch.highdim.datastd.ChromosomalRegionSupport
import org.transmartproject.batch.highdim.datastd.PlatformOrganismSupport

/**
 * Represents a line on the transcript file
 */
@Canonical
class RnaSeqTranscriptAnnotationRow {
    String gplId
    String chromosome
    Long start
    Long end
    String transcript
}
