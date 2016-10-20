package org.transmartproject.batch.highdim.rnaseq.transcript.data

import org.hamcrest.Matchers
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.AcceptAnyNumberIsCloseTo.castingCloseTo

/**
 * test RNASeq data first load
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class RnaSeqTranscriptDataCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'RNASEQ_TRANSCRIPT_ANNOT'

    private final static long NUMBER_OF_ASSAYS = 3
    private final static long NUMBER_OF_REGIONS = 3

    private final static double DELTA = 1.0E-12d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'rnaseq_transcript'),
            new RunJobRule(PLATFORM_ID, 'rnaseq_transcript_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.RNA_SEQ_TR_TABLES + 'ts_batch.batch_job_instance',)
    }

    @Test
    void testNumberOfRowsInSSM() {
        def count = rowCounter.count Tables.SUBJ_SAMPLE_MAP

        assertThat count, is(equalTo(NUMBER_OF_ASSAYS))
    }

    @Test
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.RNASEQ_TRANSCRIPT_DATA

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_REGIONS))
    }

    @Test
    void testArbitrarySample() {
        def sampleCode = 'RNASEQ.COLO205'
        def subjectId = 'COLO205'

        def q = """
                SELECT
                    PD.sourcesystem_cd as pd_sourcesystem_cd,
                    CD.concept_path as cd_concept_path,
                    assay_id,
                    sample_type,
                    trial_name,
                    tissue_type,
                    timepoint,
                    gpl_id
                FROM ${Tables.SUBJ_SAMPLE_MAP} SSM
                LEFT JOIN ${Tables.PATIENT_DIMENSION} PD ON (SSM.patient_id = PD.patient_num)
                LEFT JOIN ${Tables.CONCEPT_DIMENSION} CD ON (SSM.concept_code = CD.concept_cd)
                WHERE SSM.subject_id = :subjectId AND SSM.sample_cd = :sampleCd"""

        Map<String, Object> r = queryForMap q, [subjectId: subjectId, sampleCd: sampleCode]

        assertThat r, allOf(
                hasEntry('pd_sourcesystem_cd', "$STUDY_ID:$subjectId" as String),
                hasEntry('cd_concept_path', '\\Public Studies\\CLUC\\Test\\RnaSeqTranscript\\rcnt\\data\\'),
                Matchers.hasEntry(is('assay_id'), isA(Number)),
                hasEntry('sample_type', 'rcnt'),
                hasEntry('trial_name', STUDY_ID),
                hasEntry('tissue_type', 'Colon'),
                hasEntry('timepoint', 'tp1'),
                hasEntry('gpl_id', PLATFORM_ID),
        )
    }

    @Test
    void testArbitraryFact() {
        def q = """
                SELECT
                    D.readcount,
                    D.normalized_readcount,
                    D.log_normalized_readcount,
                    D.zscore
                FROM
                    ${Tables.RNASEQ_TRANSCRIPT_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.RNASEQ_TRANSCRIPT_ANNOTATION} A ON (D.transcript = A.id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.ref_id = :ref_id"""

        def p = [sample_name: 'RNASEQ.COLO205',
                 ref_id     : '1']

        Map<String, Object> r = queryForMap q, p

        long readcount = 47
        double normalizedReadcount = 356.35
        double logValue = Math.log(normalizedReadcount) / Math.log(2d)
        double zscore = 0.54

        assertThat r, allOf(
                hasEntry(equalTo('readcount'), castingCloseTo(readcount, DELTA)),
                hasEntry(equalTo('normalized_readcount'), castingCloseTo(normalizedReadcount, DELTA)),
                hasEntry(equalTo('log_normalized_readcount'), castingCloseTo(logValue, DELTA)),
                hasEntry(equalTo('zscore'), castingCloseTo(zscore, DELTA)),
        )
    }

}
