package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.search.SearchKeywordCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult

/**
 * Created by olafmeuwese on 13/10/16.
 */
class RnaSeqTranscriptTestData {

    static final String REGION_PLATFORM_MARKER_TYPE = "RNASEQ_TRANSCRIPT"

    static final String TRIAL_NAME = "RNASEQ_TRANSCRIPT_TRIAL"

    SampleBioMarkerTestData bioMarkerTestData

    RnaSeqTranscriptTestData(SampleBioMarkerTestData bioMarkerTestData = null) {
        this.bioMarkerTestData = bioMarkerTestData ?: new SampleBioMarkerTestData()
    }

    @Lazy
    List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.transcriptSearchKeywords
    }()


    DeGplInfo regionPlatform = {
        def p = new DeGplInfo(
                markerType: REGION_PLATFORM_MARKER_TYPE,
                genomeReleaseId: 'hg18',
        )
        p.id = -1000L
        p
    }()

    def transcripts = {
        def t = [
                new DeRnaseqTranscriptAnnotation(
                        chromosome: '1',
                        start: 100,
                        end: 200,
                        transcriptId: 'first',
                        platform: regionPlatform,
                )
        ]
        t[0].id = -100L
        t
    }()

    List<PatientDimension> patients = createTestPatients(2, -2010, TRIAL_NAME)

    QtQueryMaster allPatientsQueryResult = createQueryResult(patients)

    def assays = createTestAssays(patients, -2010L, regionPlatform, TRIAL_NAME)

    def rnaseqTranscriptData = [new DeSubjectRnaseqTranscriptData(
            transcript: transcripts[0],
            assay: assays[0],
            readcount: 10,
            normalizedReadcount: 2,
            logNormalizedReadcount: 5,
            zscore: 1.5
    )]

    void saveAll() {
        bioMarkerTestData.saveTranscriptData()
        save([regionPlatform])
        save transcripts
        save patients
        save([allPatientsQueryResult])
        save assays
        save rnaseqTranscriptData

    }
}
