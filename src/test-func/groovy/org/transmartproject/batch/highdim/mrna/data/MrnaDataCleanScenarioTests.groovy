package org.transmartproject.batch.highdim.mrna.data

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
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * test mRNA data import in the simplest scenario (good data not previously
 * loaded)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaDataCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    private final static long NUMBER_OF_ASSAYS = 8
    // the first assay only has zeros, negative numbers and NaNs
    private final static long NUMBER_OF_PROBES = 19
    private final static long NOT_SUPPORTED_VALUES = 2

    // oracle only has 4 digits to the right of the decimal point
    private final static double DELTA = 1e-4d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'expression'),
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule("${STUDY_ID}_simple", 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.MRNA_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfRowsInSSM() {
        def count = rowCounter.count Tables.SUBJ_SAMPLE_MAP,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count, is(equalTo(NUMBER_OF_ASSAYS))
    }

    @Test
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES - NOT_SUPPORTED_VALUES))
    }

    @Test
    void testArbitrarySample() {
        def sampleCode = 'GSM210006'
        def subjectId = 'GSE8581GSM210006'

        def q = """
                SELECT
                    PD.sourcesystem_cd as pd_sourcesystem_cd,
                    CD.concept_path as cd_concept_path,
                    assay_id,
                    sample_type,
                    trial_name,
                    tissue_type,
                    timepoint,
                    gpl_id,
                    sample_cd
                FROM ${Tables.SUBJ_SAMPLE_MAP} SSM
                LEFT JOIN ${Tables.PATIENT_DIMENSION} PD ON (SSM.patient_id = PD.patient_num)
                LEFT JOIN ${Tables.CONCEPT_DIMENSION} CD ON (SSM.concept_code = CD.concept_cd)
                WHERE subject_id = :subjectId"""

        Map<String, Object> r = queryForMap q, [subjectId: subjectId]

        assertThat r, allOf(
                hasEntry('pd_sourcesystem_cd', "$STUDY_ID:$subjectId" as String),
                hasEntry('cd_concept_path', '\\Public Studies\\GSE8581\\Biomarker Data\\GPL570_BOGUS\\Lung\\'),
                hasEntry(is('assay_id'), isA(Number)),
                hasEntry('sample_type', 'Human'),
                hasEntry('trial_name', STUDY_ID),
                hasEntry('tissue_type', 'Lung'),
                hasEntry('timepoint', 'T'),
                hasEntry('gpl_id', 'GPL570_BOGUS'),
                hasEntry('sample_cd', sampleCode),
        )
    }

    @Test
    void testArbitraryFact() {
        // (GSM210006, 121_at) -> 105.912
        def sampleName = 'GSM210006'
        def subjectId = 'GSE8581GSM210006'
        def probeName = '121_at'

        def q = """
                SELECT D.patient_id, D.subject_id, raw_intensity,
                       log_intensity, zscore
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.probe_id = :probe_name
                    AND D.trial_name = :study_id"""

        def p = [study_id: STUDY_ID,
                 sample_name: sampleName,
                 probe_name: probeName]

        Map<String, Object> r = queryForMap q, p

        long patientCode = jdbcTemplate.queryForObject(
                "SELECT patient_num FROM ${Tables.PATIENT_DIMENSION} " +
                        "WHERE sourcesystem_cd = :ssd",
                [ssd: "$STUDY_ID:$subjectId".toString()],
                Long)

        def stats = queryForMap """
        SELECT
            stddev_samp(log_intensity) as stddev, avg(log_intensity) as mean
        FROM ${Tables.MRNA_DATA} D
        INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
        WHERE A.probe_id = :probe_name AND D.trial_name = :study_id
        """, [ study_id: STUDY_ID, probe_name: probeName ]

        double value = 105.912d
        double logValue = Math.log(value) / Math.log(2d)

        assertThat r, allOf(
                hasEntry(is('patient_id'), isIntegerNumber(patientCode)),
                hasEntry('subject_id', 'GSE8581GSM210006'),
                hasEntry(is('raw_intensity'), castingCloseTo(value, DELTA)),
                hasEntry(is('log_intensity'), castingCloseTo(logValue, DELTA)),
                hasEntry(is('zscore'),
                        castingCloseTo((logValue - stats.mean) / stats.stddev, DELTA)),
        )
    }

    @Test
    void testRawNANsAndNegativesAreOmitted() {
        def sampleName = 'GSM210005'
        def q = """
                SELECT raw_intensity, log_intensity, zscore
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND D.trial_name = :study_id
                    AND raw_intensity != 0"""
        def p = [study_id: STUDY_ID,
                 sample_name: sampleName]

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, is(empty())
    }

    @Test
    void testLogOf0Workaround() {

        def minPos = queryForMap """
                SELECT min(D.raw_intensity) as val
                FROM
                    ${Tables.MRNA_DATA} D
                WHERE
                    D.trial_name = :study_id and D.raw_intensity > 0""",
                [study_id: STUDY_ID]

        def logs = queryForList """
                SELECT D.log_intensity as log
                FROM
                    ${Tables.MRNA_DATA} D
                WHERE
                    D.trial_name = :study_id and D.raw_intensity = 0""",
                [study_id: STUDY_ID]

        def logTrick = Math.log(minPos.val / 2) / Math.log(2)

        assertThat logs, everyItem(
                hasEntry(equalTo('log'), castingCloseTo(logTrick, DELTA))
        )
    }
}
