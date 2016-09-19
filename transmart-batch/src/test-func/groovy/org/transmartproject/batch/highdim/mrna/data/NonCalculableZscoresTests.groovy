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

/**
 * test calculation of z-scores when data set values make it un-calculable (std_div = 0; division by zero)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class NonCalculableZscoresTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'NOZSCORE'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    @Test
    void testZscoresAreNulls() {
        List r = queryForList """
                SELECT D.zscore
                FROM
                    ${Tables.MRNA_DATA} D
                INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE D.trial_name = :study_id AND A.probe_id NOT LIKE 'AFFX-HUMISGF3A%'""",
                [study_id: STUDY_ID]

        assertThat r, everyItem(
                hasEntry(is('zscore'), nullValue()),
        )
    }

    @Test
    void testZscoresAreNotNulls() {
        List r = queryForList """
                SELECT D.zscore
                FROM
                    ${Tables.MRNA_DATA} D
                INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE D.trial_name = :study_id AND A.probe_id LIKE 'AFFX-HUMISGF3A%'""",
                [study_id: STUDY_ID]

        assertThat r, everyItem(
                hasEntry(is('zscore'), not(nullValue())),
        )
    }

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'expression'),
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.MRNA_TABLES + 'ts_batch.batch_job_instance',)
    }

}
