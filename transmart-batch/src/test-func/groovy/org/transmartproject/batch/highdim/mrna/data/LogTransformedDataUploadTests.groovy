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
 * test mRNA data import with DATA_TYPE=L flag.
 * Which means that file contains log transformed values instead of raw ones.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class LogTransformedDataUploadTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'
    private final static long NUMBER_OF_ASSAYS = 1
    private final static long NUMBER_OF_PROBES = 19
    private final static long NA_VALUES = 2

    private final static double DELTA = 1e-4d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule("studies/${STUDY_ID}/log_transformed/expression.params"),
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
    void testIntensityMatches() {
        def sampleName = 'GSM210012'
        def probeName = '121_at'
        double log10Intensity = 2.0373547737

        assertLogTransformation sampleName, probeName, log10Intensity
    }

    @Test
    void testNegativeIntensityMatches() {
        def sampleName = 'GSM210012'
        def probeName = '1320_at'

        double log10Intensity = -0.4030260991

        assertLogTransformation sampleName, probeName, log10Intensity
    }

    @Test
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES - NA_VALUES))
    }

    def assertLogTransformation(String sampleName, String probeName, double log10Intensity) {
        def rows = queryForList """
                SELECT
                    D.raw_intensity,
                    D.log_intensity
                FROM
                    ${Tables.MRNA_ANNOTATION} A
                INNER JOIN ${Tables.MRNA_DATA} D ON (D.probeset_id = A.probeset_id)
                INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.probe_id = :probe_name
                    AND D.trial_name = :study_id""",
                [study_id   : STUDY_ID,
                 sample_name: sampleName,
                 probe_name : probeName]

        double origRawValue = Math.pow(10, log10Intensity)
        double log2Intensity = Math.log(origRawValue) / Math.log(2d)

        assertThat rows, everyItem(
                allOf(
                        hasEntry(equalTo('raw_intensity'), castingCloseTo(origRawValue, DELTA)),
                        hasEntry(equalTo('log_intensity'), castingCloseTo(log2Intensity, DELTA)),
                )
        )
    }
}
