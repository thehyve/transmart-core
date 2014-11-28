package org.transmartproject.batch.highdim.mrna.data

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.NoSkipIfJobFailed
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.junit.SkipIfJobFailedRule

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * test mRNA data import in the simplest scenario (good data not previously
 * loaded)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaDataCleanScenarioTests {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    private final static long NUMBER_OF_ASSAYS = 7
    private final static long NUMBER_OF_PROBES = 19

    private final static double ALL_DATA_MEAN = 98.97085804511279
    private final static double ALL_DATA_STD_DEV = 114.39026987889345
    private final static double DELTA = 1e-10d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'expression'),
            new RunJobRule(PLATFORM_ID, 'annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    @Autowired
    JobRepository jobRepository

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobFailedRule = new SkipIfJobFailedRule(
            jobRepositoryProvider: { -> jobRepository },
            runJobRule: RUN_JOB_RULES.rulesStartingWithInnerMost[0],
            jobName: MrnaDataJobConfiguration.JOB_NAME,)

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(
                        Tables.OBSERVATION_FACT,
                        Tables.CONCEPT_DIMENSION,
                        Tables.PATIENT_TRIAL,
                        Tables.PATIENT_DIMENSION,
                        Tables.I2B2,
                        Tables.I2B2_SECURE,
                        Tables.I2B2_TAGS,
                        "${Tables.GPL_INFO} CASCADE",
                        Tables.MRNA_ANNOTATION,
                        'ts_batch.batch_job_instance CASCADE',
                        Tables.SUBJ_SAMPLE_MAP,
                        Tables.MRNA_DATA,
                )
    }

    @Test
    @NoSkipIfJobFailed
    void testJobCompletedSuccessfully() {
        assert skipIfJobFailedRule.jobCompletedSuccessFully,
                'The job completed successfully'
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
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count, is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES))
    }

    @Test
    void testRandomFact() {
        // (GSM210006, 121_at) -> 105.912
        def sampleName = 'GSM210006'
        def subjectId = 'GSE8581GSM210006'
        def probeName = '121_at'

        def q = """
                SELECT D.patient_id, D.subject_id, raw_intensity,
                       log_intensity, zscore
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.MRNA_ANNOTATION} A ON (D.probeset_id = A.probeset_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.probe_id = :probe_name
                    AND D.trial_name = :study_id"""

        def p = [study_id: STUDY_ID,
                 sample_name: sampleName,
                 probe_name: probeName]

        Map<String, Object> r = jdbcTemplate.queryForMap q, p

        long patientCode = jdbcTemplate.queryForObject(
                "SELECT patient_num FROM ${Tables.PATIENT_DIMENSION} " +
                        "WHERE sourcesystem_cd = :ssd",
                [ssd: "$STUDY_ID:$subjectId".toString()],
                Long)

        assertThat r, allOf(
                hasEntry('patient_id', patientCode),
                hasEntry('subject_id', 'GSE8581GSM210006'),
                hasEntry(is('raw_intensity'), closeTo(105.912d, DELTA)),
                hasEntry(is('log_intensity'),
                        closeTo(Math.log(105.912d) / Math.log(2d), DELTA)),
                hasEntry(is('zscore'),
                        closeTo((105.912d - ALL_DATA_MEAN) / ALL_DATA_STD_DEV, DELTA)),
        )

    }


}
