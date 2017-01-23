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

/**
 * test mRNA data import in the simplest scenario (good data not previously
 * loaded)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class CustomZscoresTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    private final static long NUMBER_OF_ASSAYS = 2
    // the first assay only has zeros, negative numbers and NaNs
    private final static long NUMBER_OF_PROBES = 19
    private final static long NOT_SUPPORTED_VALUES = 2

    // oracle only has 4 digits to the right of the decimal point
    private final static double DELTA = 1e-4d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule("${STUDY_ID}_custom_zscores", 'expression', ['-d', 'STUDY_ID=' + STUDY_ID]),
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
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES - NOT_SUPPORTED_VALUES))
    }

    @Test
    void testArbitraryFact() {
        def sampleName = 'GSM210006'
        def probeName = '1431_at'

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

        def p = [study_id   : STUDY_ID,
                 sample_name: sampleName,
                 probe_name : probeName]

        Map<String, Object> r = queryForMap q, p
        double value = 10.904d
        double logValue = Math.log(value) / Math.log(2d)
        double zscore = 0.564d

        assertThat r, allOf(
                hasEntry(is('raw_intensity'), castingCloseTo(value, DELTA)),
                hasEntry(is('log_intensity'), castingCloseTo(logValue, DELTA)),
                hasEntry(is('zscore'), castingCloseTo(zscore, DELTA)),
        )
    }

}
