package org.transmartproject.batch.highdim.rnaseq.data

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
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

/**
 * test RNASeq data re-upload
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class RnaSeqDataReUploadTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'RNASEQ_ANNOT'

    private final static long NUMBER_OF_ASSAYS = 3
    private final static long NUMBER_OF_REGIONS = 4


    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'rnaseq', ['-n']),
            new RunJobRule(STUDY_ID, 'rnaseq'),
            new RunJobRule(PLATFORM_ID, 'rnaseq_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.RNA_SEQ_TABLES + 'ts_batch.batch_job_instance',)
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
        def count = rowCounter.count Tables.RNASEQ_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_REGIONS))
    }
}
