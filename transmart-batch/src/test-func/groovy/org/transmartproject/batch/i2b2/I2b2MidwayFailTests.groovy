package org.transmartproject.batch.i2b2

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.junit.FileCorruptingTestTrait
import org.transmartproject.batch.junit.LoadTablesRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

/**
 * Test a failure in the middle of the second pass of an i2b2 job run.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class I2b2MidwayFailTests implements FileCorruptingTestTrait {

    public final static String DATA_DIR = 'I2B2SAMPLE'

    public final static String SOURCE_SYSTEM = 'TEST_DATA'

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @ClassRule
    public final static TestRule LOAD_CONCEPTS_RULE = new LoadTablesRule(
            (Tables.I2B2): new ClassPathResource('i2b2/i2b2.tsv'),
            (Tables.CONCEPT_DIMENSION): new ClassPathResource('i2b2/concept_dimension.tsv'))

    @BeforeClass
    static void beforeClass() {
        I2b2JobConfiguration.secondPassChunkSize = 2
    }

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.I2B2_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testFailMidwaySecondStep() {
        def params = ['-p', 'studies/' + DATA_DIR + '/i2b2.params',]

        jdbcTemplate.update("ALTER TABLE $Tables.OBSERVATION_FACT " +
                "ADD CONSTRAINT test_constraint " +
                "CHECK (tval_char <> 'text of patient 3')", [:])
        try {
            firstExecution(params)
        } finally {
            jdbcTemplate.update("ALTER TABLE $Tables.OBSERVATION_FACT " +
                    "DROP CONSTRAINT test_constraint", [:])
        }

        secondExecution(params)

        assertThat rowCounter.count(Tables.OBSERVATION_FACT),
                is(I2b2NonIncrementalTests.NUMBER_OF_FACTS +
                        I2b2NonIncrementalTests.NUMBER_OF_MODIFIER_FACTS)
    }

}
