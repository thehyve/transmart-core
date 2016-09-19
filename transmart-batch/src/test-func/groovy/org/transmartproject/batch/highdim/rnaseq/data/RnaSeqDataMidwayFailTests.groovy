package org.transmartproject.batch.highdim.rnaseq.data

import com.google.common.io.Files
import org.junit.*
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.FileCorruptingTestTrait
import org.transmartproject.batch.junit.NoSkipIfJobFailed
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.junit.SkipIfJobFailedRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataCleanScenarioTests.NUMBER_OF_ASSAYS
import static org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataCleanScenarioTests.NUMBER_OF_REGIONS
import static org.transmartproject.batch.matchers.AcceptAnyNumberIsCloseTo.castingCloseTo
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * For rnaseq, test a failure midway the first pass, and then restart the job with
 * the problem fixed. Then the same for the second pass.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class RnaSeqDataMidwayFailTests implements FileCorruptingTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'RNASEQ_ANNOT'

    private final static double DELTA = 1.0E-12

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    TableTruncator truncator

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'rnaseq_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobsFailed =
            new SkipIfJobFailedRule(runJobRule: RUN_JOB_RULES)

    File originalFile = new File("studies/${STUDY_ID}/rnaseq/rnaseq_data.tsv")

    @BeforeClass
    static void beforeClass() {
        RnaSeqDataStepsConfig.dataFilePassChunkSize = 2
    }

    @AfterClass
    static void cleanDatabase() {
        RnaSeqDataStepsConfig.dataFilePassChunkSize = 10000
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + Tables.CHROMOSOMAL_REGION
                        + [Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @After
    void cleanData() {
        truncator.truncate([Tables.RNASEQ_DATA, Tables.SUBJ_SAMPLE_MAP])
    }

    @Test
    @NoSkipIfJobFailed
    void testPrerequisitesPassed() {
        assert skipIfJobsFailed.jobCompletedSuccessFully,
                'The pre-requisite jobs completed successfully'
    }

    @Test
    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    void testFailMidwayFirstStep() {
        File dataFile = corruptFile(originalFile, 2, 3, 'CORRUPTION')

        def params = [
                '-p', 'studies/' + STUDY_ID + '/rnaseq.params',
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath]

        // first execution
        firstExecution(params)

        // fix the file
        Files.copy(originalFile, dataFile)
        secondExecution(params)

        checkFactCount()
    }

    @Test
    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    void testFailMidwayFirstStepValidation() {
        File dataFile = corruptFile(originalFile, 2, 3, '')

        def params = [
                '-p', 'studies/' + STUDY_ID + '/rnaseq.params',
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath]

        // first execution
        firstExecution(params)

        // fix the file
        Files.copy(originalFile, dataFile)
        secondExecution(params)

        checkFactCount()
    }

    @Test
    void testFailMidwaySecondStep() {
        def params = [
                '-p', 'studies/' + STUDY_ID + '/rnaseq.params',
                '-d', 'DATA_FILE_PREFIX=' + originalFile.absolutePath]

        long readCount = 47
        double normalizedReadCount = 356.35

        jdbcTemplate.update("ALTER TABLE $Tables.RNASEQ_DATA " +
                "ADD CONSTRAINT test_constraint " +
                "CHECK (readcount <> ${readCount})", [:])
        try {
            firstExecution(params)
        } finally {
            jdbcTemplate.update("ALTER TABLE $Tables.RNASEQ_DATA " +
                    "DROP CONSTRAINT test_constraint", [:])
        }

        secondExecution(params)

        checkFactCount()

        // check the fact that was corrupted the first time
        def logValue = Math.log(normalizedReadCount) / Math.log(2d)

        def q = """
                SELECT readcount, normalized_readcount, log_normalized_readcount
                FROM
                    ${Tables.RNASEQ_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.CHROMOSOMAL_REGION} A ON (D.region_id = A.region_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.region_name = :region_name
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: 'RNASEQ.COLO205',
                 region_name: 'FAM138F']

        Map<String, Object> r = jdbcTemplate.queryForMap q, p

        assertThat r, allOf(
                hasEntry(equalToIgnoringCase('readcount'), isIntegerNumber(readCount)),
                hasEntry(equalToIgnoringCase('normalized_readcount'), castingCloseTo(normalizedReadCount, DELTA)),
                hasEntry(equalToIgnoringCase('log_normalized_readcount'), castingCloseTo(logValue, DELTA)),
        )
    }

    private void checkFactCount() {
        def count = rowCounter.count Tables.RNASEQ_DATA, 'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_REGIONS))
    }
}
