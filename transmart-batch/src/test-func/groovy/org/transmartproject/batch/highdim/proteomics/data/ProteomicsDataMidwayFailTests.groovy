package org.transmartproject.batch.highdim.proteomics.data

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
import static org.transmartproject.batch.highdim.proteomics.data.ProteomicsDataCleanScenarioTests.NUMBER_OF_ASSAYS
import static org.transmartproject.batch.highdim.proteomics.data.ProteomicsDataCleanScenarioTests.NUMBER_OF_PROBES

/**
 * For proteomics, test a failure midway the first pass, and then restart the job with
 * the problem fixed. Then the same for the second pass.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ProteomicsDataMidwayFailTests implements FileCorruptingTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'PROT_ANNOT'

    private final static BigDecimal DELTA = 1.0E-12

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    TableTruncator truncator

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'proteomics_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobsFailed =
            new SkipIfJobFailedRule(runJobRule: RUN_JOB_RULES)

    File originalFile = new File("studies/${STUDY_ID}/proteomics/proteomics_data.txt")

    @BeforeClass
    static void beforeClass() {
        ProteomicsDataStepsConfig.dataFilePassChunkSize = 2
    }

    @AfterClass
    static void cleanDatabase() {
        ProteomicsDataStepsConfig.dataFilePassChunkSize = 10000
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + Tables.PROTEOMICS_ANNOTATION
                        + [Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @After
    void cleanData() {
        truncator.truncate([Tables.PROTEOMICS_DATA, Tables.SUBJ_SAMPLE_MAP])
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
        File dataFile = corruptFile(originalFile, 7, 5, 'CORRUPTION')

        def params = [
                '-p', 'studies/' + STUDY_ID + '/proteomics.params',
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
                '-p', 'studies/' + STUDY_ID + '/proteomics.params',
                '-d', 'DATA_FILE_PREFIX=' + originalFile.absolutePath]

        def value = new BigDecimal(5527800000)
        jdbcTemplate.update("ALTER TABLE $Tables.PROTEOMICS_DATA " +
                "ADD CONSTRAINT test_constraint " +
                "CHECK (intensity <> ${value})", [:])
        try {
            firstExecution(params)
        } finally {
            jdbcTemplate.update("ALTER TABLE $Tables.PROTEOMICS_DATA " +
                    "DROP CONSTRAINT test_constraint", [:])
        }

        secondExecution(params)

        checkFactCount()

        // check the fact that was corrupted the first time
        def logValue = new BigDecimal(Math.log(value) / Math.log(2d))

        def q = """
                SELECT intensity, log_intensity
                FROM
                    ${Tables.PROTEOMICS_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.PROTEOMICS_ANNOTATION} A ON (D.protein_annotation_id = A.id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.peptide = :peptide
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: 'LFQ.intensity.CACO2_2',
                 peptide    : '2243']

        Map<String, Object> r = jdbcTemplate.queryForMap q, p

        assertThat r, allOf(
                hasEntry(equalToIgnoringCase('intensity'), closeTo(value, DELTA)),
                hasEntry(equalToIgnoringCase('log_intensity'), closeTo(logValue, DELTA)),
        )
    }

    private void checkFactCount() {
        def count = rowCounter.count Tables.PROTEOMICS_DATA, 'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES))
    }
}
