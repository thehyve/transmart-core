package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.search.SearchKeywordCoreDb

import java.text.SimpleDateFormat

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
/**
 * Created by olafmeuwese on 13/10/16.
 */
class RnaSeqTranscriptTestData {

    static final String REGION_PLATFORM_MARKER_TYPE = "RNASEQ_TRANSCRIPT"

    static final String TRIAL_NAME = "RNASEQ_TRANSCRIPT_TRIAL"

    private SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd')

    SampleBioMarkerTestData bioMarkerTestData

    RnaSeqTranscriptTestData(SampleBioMarkerTestData bioMarkerTestData = null) {
        this.bioMarkerTestData = bioMarkerTestData ?: new SampleBioMarkerTestData()
    }

    @Lazy
    List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.transcriptSearchKeywords +
                bioMarkerTestData.geneSearchKeywords
    }()


    DeGplInfo regionPlatform = {
        def p = new DeGplInfo(
                title: 'Test Region Platform',
                organism: 'Homo Sapiens',
                annotationDate: sdf.parse('2013-05-03'),
                markerType: REGION_PLATFORM_MARKER_TYPE,
                genomeReleaseId: 'hg18',
        )
        p.id = 'test-region-platform_rnaseq'
        p
    }()

    def transcripts = {
        def t = [
                new DeRnaseqTranscriptAnnot(
                        chromosome: '1',
                        refId: 'ref1',
                        start: 33,
                        end: 9999,
                        transcript: 'foo_transcript_1',
                        platform: regionPlatform
                ),
                new DeRnaseqTranscriptAnnot(
                        chromosome: '2',
                        refId: 'ref2',
                        start: 66,
                        end: 99,
                        transcript: 'foo_transcript_2',
                        platform: regionPlatform
                ),
                new DeRnaseqTranscriptAnnot(
                        chromosome: '3',
                        refId: 'ref2',
                        start: 150,
                        end: 300,
                        transcript: 'foo_transcript_3',
                        platform: regionPlatform
                )
        ]
        t[0].id = -100L
        t[1].id = -101L
        t[2].id = -102L
        t
    }()

    List<PatientDimension> patients = createTestPatients(2, -2010, TRIAL_NAME)

    @Lazy
    QtQueryMaster allPatientsQueryResult = {
        createQueryResult('rnaseqtranscript-patients-set', patients)
    }()

    def assays = createTestAssays(patients, -2010L, regionPlatform, TRIAL_NAME)


    DeRnaseqTranscriptData createRNASEQTranscriptData(DeRnaseqTranscriptAnnot transcript,
                                                      Assay assay,
                                                      readcount = 0,
                                                      normalizedreadcount = 0.0) {
        new DeRnaseqTranscriptData(
                transcript: transcript,
                assay: assay,
                patient: assay.patient,
                readcount: readcount,
                normalizedReadcount: normalizedreadcount,
                logNormalizedReadcount: Math.log(normalizedreadcount) / Math.log(2.0),
                zscore: ((Math.log(normalizedreadcount) / Math.log(2.0)) - 0.5) / 1.5,
        )
    }

    List<DeRnaseqTranscriptData> rnaseqTranscriptData = {
        [
                createRNASEQTranscriptData(transcripts[0], assays[0], 1, 1.0),
                createRNASEQTranscriptData(transcripts[0], assays[1], 10, 4.0),
                createRNASEQTranscriptData(transcripts[1], assays[0], 2, 0.5),
                createRNASEQTranscriptData(transcripts[1], assays[1], 2, 2.0),
                createRNASEQTranscriptData(transcripts[2], assays[0], 2, 0.5),
                createRNASEQTranscriptData(transcripts[2], assays[1], 2, 2.0)
        ]
    }()

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
