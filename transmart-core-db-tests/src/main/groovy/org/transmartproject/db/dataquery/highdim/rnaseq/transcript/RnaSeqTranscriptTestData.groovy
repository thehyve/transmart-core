package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import org.transmartproject.db.dataquery.highdim.DeGplInfo

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestAssays
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestPatients

/**
 * Created by olafmeuwese on 13/10/16.
 */

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class RnaSeqTranscriptTestData {

    static final String REGION_PLATFORM_MARKER_TYPE = "RNASEQ_TRANSCRIPT"

    static final String TRIAL_NAME = "RNASEQ_TRANSCRIPT_TRIAL"

    void saveAll() {
        def regionPlatform = new DeGplInfo(
                markerType: REGION_PLATFORM_MARKER_TYPE,
                genomeReleaseId: 'hg18'
        )
        regionPlatform.id = -1000L
        def transcripts = [
                new DeRnaseqTranscriptAnnotation(
                        chromosome: '1',
                        start: 100,
                        end: 200,
                        transcriptId: 'first',
                        platform: regionPlatform
                )
        ]
        transcripts[0].id = -100L

        def patients = createTestPatients(1, -1001L, TRIAL_NAME)
        def assays = createTestAssays(patients, -2010, regionPlatform, TRIAL_NAME)

        def rnaseqTranscriptData = [new DeSubjectRnaseqTranscriptData(
                transcript: transcripts[0],
                assay: assays[0],
                readcount: 10,
                normalizedReadcount: 2,
                logNormalizedReadcount: 5,
                zscore: 1.5


        )]
        save([regionPlatform])
        save transcripts
        save patients
        save assays
        save rnaseqTranscriptData

    }
}
