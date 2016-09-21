package org.transmartproject.batch.highdim.mrna.data

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
import static org.transmartproject.batch.highdim.mrna.data.MrnaDataCleanScenarioTests.*
import static org.transmartproject.batch.matchers.AcceptAnyNumberIsCloseTo.castingCloseTo

/**
 * For mRNA, test a failure midway the first pass, and then restart the job with
 * the problem fixed. Then the same for the second pass.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaDataMidwayFailTests implements FileCorruptingTestTrait {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    // oracle schema only has scale = 4 on the numeric columns
    private final static double DELTA = 1e-4d

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    TableTruncator truncator

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule("${STUDY_ID}_simple", 'clinical'),
    ])

    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobsFailed =
            new SkipIfJobFailedRule(runJobRule: RUN_JOB_RULES)

    File originalFile = new File('studies/GSE8581/expression/GSE8581_series_matrix.txt')

    @BeforeClass
    static void beforeClass() {
        MrnaDataStepsConfig.dataFilePassChunkSize = 2
    }

    @AfterClass
    static void cleanDatabase() {
        MrnaDataStepsConfig.dataFilePassChunkSize = 10000
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + [Tables.MRNA_ANNOTATION,
                                                       Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @After
    void cleanData() {
        truncator.truncate([Tables.MRNA_DATA, Tables.SUBJ_SAMPLE_MAP])
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
        File dataFile = corruptFile(originalFile, 9, 3, 'CORRUPTION')

        def params = [
                '-p', 'studies/' + STUDY_ID + '/expression.params',
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath]

        // first execution
        firstExecution(params)

        // fix the file
        Files.copy(originalFile, dataFile)
        secondExecution(params)

        checkFactCount()
        checkLog0Workaround()
    }

    @Test
    void testFailMidwaySecondStep() {
        def params = [
                '-p', 'studies/' + STUDY_ID + '/expression.params',
                '-d', 'DATA_FILE_PREFIX=' + originalFile.absolutePath]

        double value = 44.1729d
        jdbcTemplate.update("ALTER TABLE $Tables.MRNA_DATA " +
                "ADD CONSTRAINT test_constraint " +
                "CHECK (raw_intensity <> ${value})", [:])
        try {
            firstExecution(params)
        } finally {
            jdbcTemplate.update("ALTER TABLE $Tables.MRNA_DATA " +
                    "DROP CONSTRAINT test_constraint", [:])
        }

        secondExecution(params)

        checkFactCount()
        checkLog0Workaround()

        def probeName = '1316_at'
        // check the fact that was corrupted the first time
        double logValue = Math.log(value) / Math.log(2d)
        def stats = jdbcTemplate.queryForMap """
        SELECT
            stddev_samp(log_intensity) as stddev, avg(log_intensity) as mean
        FROM ${Tables.MRNA_DATA} D
        INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
        WHERE A.probe_id = :probe_name AND D.trial_name = :study_id
        """, [study_id: STUDY_ID, probe_name: probeName]

        def q = """
                SELECT raw_intensity, log_intensity, zscore
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.probe_id = :probe_name
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: 'GSM210010',
                 probe_name : probeName]

        Map<String, Object> r = jdbcTemplate.queryForMap q, p

        assertThat r, allOf(
                hasEntry(equalToIgnoringCase('raw_intensity'), castingCloseTo(value, DELTA)),
                hasEntry(equalToIgnoringCase('log_intensity'), castingCloseTo(logValue, DELTA)),
                hasEntry(equalToIgnoringCase('zscore'),
                        castingCloseTo((logValue - stats.mean) / stats.stddev, DELTA)),
        )
    }

    private void checkFactCount() {
        def count = rowCounter.count Tables.MRNA_DATA, 'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES - NOT_SUPPORTED_VALUES))
    }

    private void checkLog0Workaround() {
        //Check that log2(0) workaround (log2(minPositiveValueOfTheDataSet / 2)) is calculated based
        // on right minimum positive value.

        def minPositiveValueOfTheDataSet = 1.15958d
        double logTrickValue = Math.log(minPositiveValueOfTheDataSet / 2) / Math.log(2)
        def logs = jdbcTemplate.queryForList """
          SELECT D.log_intensity as log
                FROM
                    ${Tables.MRNA_DATA} D
                WHERE
                    D.trial_name = :study_id and D.raw_intensity = 0
        """, [study_id: STUDY_ID]

        assertThat """Either minimal positive value of the data set has not been found right after recovery process
                    or log2(0) is not substituted with log2(minPositiveValueOfTheDataSet / 2).""",
                logs, everyItem(
                hasEntry(equalToIgnoringCase('log'), castingCloseTo(logTrickValue, DELTA))
        )
    }
}
