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
class ZeroMeansNoInfoTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'ZERO_MEANS_NO_INFO'

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule("studies/${STUDY_ID}/rnaseq.params"),
            new RunJobRule('RNASEQ_ANNOT', 'rnaseq_annotation'),

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
                        + TableLists.RNA_SEQ_TABLES
                        + 'ts_batch.batch_job_instance')
    }

    @Test
    void testMrnaProbeRowWithOnlyZerosIsNotInserted() {
        def q = """
                SELECT S.sample_cd, A.probe_id
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                WHERE D.trial_name = :study_id"""

        List<Map<String, Object>> r = queryForList q, [study_id: STUDY_ID]

        assertThat r, containsInAnyOrder(
                allOf(hasEntry(equalTo('sample_cd'), equalTo('MRNA.S11')),
                        hasEntry(equalTo('probe_id'), equalTo('1053_at'))),
                allOf(hasEntry(equalTo('sample_cd'), equalTo('MRNA.S11')),
                        hasEntry(equalTo('probe_id'), equalTo('121_at'))),
                allOf(hasEntry(equalTo('sample_cd'), equalTo('MRNA.S11')),
                        hasEntry(equalTo('probe_id'), equalTo('1294_at'))),
                allOf(hasEntry(equalTo('sample_cd'), equalTo('MRNA.S11')),
                        hasEntry(equalTo('probe_id'), equalTo('1320_at'))),
        )
    }

    @Test
    void testRnaseqRegionRowWithOnlyZerosIsNotInserted() {
        def q = """
                SELECT S.sample_cd, A.region_name
                FROM
                    ${Tables.RNASEQ_DATA} D
                    INNER JOIN ${Tables.CHROMOSOMAL_REGION} A ON (D.region_id = A.region_id)
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                WHERE D.trial_name = :study_id"""

        List<Map<String, Object>> r = queryForList q, [study_id: STUDY_ID]

        assertThat r, containsInAnyOrder(
                allOf(hasEntry(equalTo('sample_cd'), equalTo('RNASEQ.S11')),
                        hasEntry(equalTo('region_name'), equalTo('FAM138F'))),
                allOf(hasEntry(equalTo('sample_cd'), equalTo('RNASEQ.S11')),
                        hasEntry(equalTo('region_name'), equalTo('LOC100132287'))),
                allOf(hasEntry(equalTo('sample_cd'), equalTo('RNASEQ.S11')),
                        hasEntry(equalTo('region_name'), equalTo('OR4F3'))),
        )
    }

}
