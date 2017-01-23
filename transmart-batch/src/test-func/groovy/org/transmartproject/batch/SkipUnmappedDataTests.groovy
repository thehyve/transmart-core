package org.transmartproject.batch

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * test skipping data without subject sample mapping.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class SkipUnmappedDataTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'SKIP_UNMAPPED_DATA'

    private final static long NUMBER_OF_MRNA_PROBES = 19
    private final static long NUMBER_OF_CNV_PROBES = 4
    private final static long NUMBER_OF_RNASEQ_PROBES = 4

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule("studies/${STUDY_ID}/rnaseq.params"),
            new RunJobRule('RNASEQ_ANNOT', 'rnaseq_annotation'),

            new RunJobRule("studies/${STUDY_ID}/cnv.params"),
            new RunJobRule('CNV_ANNOT', 'cnv_annotation'),

            new RunJobRule("studies/${STUDY_ID}/expression.params"),
            new RunJobRule('GPL570_bogus', 'mrna_annotation'),

            new RunJobRule("studies/${STUDY_ID}/clinical.params"),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(TableLists.CLINICAL_TABLES
                        + TableLists.MRNA_TABLES
                        + TableLists.CNV_TABLES
                        + TableLists.RNA_SEQ_TABLES
                        + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfRowsInSSM() {
        def q = "SELECT sample_cd FROM ${Tables.SUBJ_SAMPLE_MAP} WHERE trial_name = :study_id"

        def list = queryForList q, [study_id: STUDY_ID], String

        assertThat list, containsInAnyOrder('MRNA.S21', 'CNV.S21', 'RNASEQ.S21')
    }

    @Test
    void testNumberOfMrnaDataPoint() {
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_MRNA_PROBES))
    }

    @Test
    void testNumberOfCnvDataPoint() {
        def count = rowCounter.count Tables.CNV_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_CNV_PROBES))
    }

    @Test
    void testNumberOfRnaSeqDataPoint() {
        def count = rowCounter.count Tables.RNASEQ_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_RNASEQ_PROBES))
    }

}
