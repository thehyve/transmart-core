package org.transmartproject.batch.highdim.mirna.data

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
 * test miRNA data import in the simplest scenario (good data not previously
 * loaded)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MirnaDataCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'CLUC'

    private final static long NUMBER_OF_ASSAYS = 2
    private final static long NUMBER_OF_PROBES = 30

    // oracle only has 4 digits to the right of the decimal point
    private final static double DELTA = 1e-4d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'mirna'),
            new RunJobRule('studies/CLUC_shortened_mirna_annotation/mirna_annotation.params'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.MIRNA_TABLES + 'ts_batch.batch_job_instance')
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
        def count = rowCounter.count Tables.MIRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES))
    }

    @Test
    void testArbitraryFact() {
        def sampleName = 'VCaP'
        def idRef = '15'

        def q = """
                SELECT raw_intensity,
                       log_intensity, zscore
                FROM
                    ${Tables.MIRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.MIRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.id_ref = :id_ref
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: sampleName,
                 id_ref     : idRef]

        Map<String, Object> r = queryForMap q, p

        assertThat r, allOf(
                Matchers.hasEntry(is('raw_intensity'), notNullValue()),
                Matchers.hasEntry(is('log_intensity'), castingCloseTo(-1.11218d, DELTA)),
                Matchers.hasEntry(is('zscore'),
                        notNullValue()),
        )
    }

}
