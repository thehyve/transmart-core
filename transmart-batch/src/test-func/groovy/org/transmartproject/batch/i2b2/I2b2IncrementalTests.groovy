package org.transmartproject.batch.i2b2

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.LoadTablesRule
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Runs the non-incremental i2b2 job, then the incremental one and queries the
 * state
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class I2b2IncrementalTests implements JobRunningTestTrait {

    public final static String DATA_DIR = 'I2B2SAMPLE_INCR'

    public final static String SOURCE_SYSTEM = 'TEST_DATA'

    private final static long NUMBER_OF_NEW_VISITS = 2

    private final static long NUMBER_OF_NEW_FACTS = 2

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RuleChain([
            new RunJobRule(I2b2IncrementalTests.DATA_DIR, 'i2b2'),
            new RunJobRule(I2b2NonIncrementalTests.DATA_DIR, 'i2b2'),
            new LoadTablesRule(
                    (Tables.I2B2): new ClassPathResource('i2b2/i2b2.tsv'),
                    (Tables.CONCEPT_DIMENSION): new ClassPathResource('i2b2/concept_dimension.tsv')),
    ])

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.I2B2_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfPatients() {
        // No new patients should have been added
        assertThat rowCounter.count(Tables.PATIENT_MAPPING),
                is(I2b2NonIncrementalTests.NUMBER_OF_PATIENTS)

    }

    @Test
    void testNumberOfFacts() {
        long count = rowCounter.count(Tables.OBSERVATION_FACT,
                'modifier_cd = :m', m: '@')
        assertThat count,
                is(I2b2NonIncrementalTests.NUMBER_OF_FACTS + NUMBER_OF_NEW_FACTS)
    }

    @Test
    void testNumberOfVisits() {
        // There should be two extra visits
        def result = queryForList(
                """SELECT encounter_ide FROM $Tables.ENCOUNTER_MAPPING""",
                [:],
                String)

        assertThat result, allOf(
                hasSize(I2b2NonIncrementalTests.NUMBER_OF_VISITS
                        + NUMBER_OF_NEW_VISITS as int),
                hasItems(
                        'patient 1@2015-04-14',
                        'patient 2@2015-04-14',
                ))
    }

}
