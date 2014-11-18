package org.transmartproject.batch.clinical

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.startup.RunJob

import java.util.concurrent.TimeUnit

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assume.assumeTrue
import static uk.co.it.modular.hamcrest.date.DateMatchers.within

/**
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ClinicalDataCleanScenarioTests {

    static JobParameters jobParameters

    static Date endImportDate = new Date()

    public static final String STUDY_ID = 'GSE8581'

    public static final NUMBER_OF_PATIENTS = 58L

    public static final NUMBER_OF_CATEGORICAL_VARIABLES = 5L
    public static final NUMBER_OF_NUMERICAL_VARIABLES = 4L
    public static final NUMBER_OF_VARIABLES =
            NUMBER_OF_CATEGORICAL_VARIABLES + NUMBER_OF_NUMERICAL_VARIABLES

    private static final BigDecimal DELTA = 0.005

    @Autowired
    JobRepository jobRepository

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    RowCounter rowCounter

    @BeforeClass
    static void loadStudy() {
        CommandLineJobRunner.presetSystemExiter({ int it -> } as SystemExiter)
        def runJob = RunJob.createInstance(
                '-p', 'studies/' + STUDY_ID + '/clinical.params')
        runJob.run()

        jobParameters = runJob.finalJobParameters
        endImportDate = new Date()
    }

    @AfterClass
    static void cleanDatabase() {
        // TODO: implement backout study and call it here
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(
//                        Tables.OBSERVATION_FACT,
//                        Tables.CONCEPT_DIMENSION,
//                        Tables.PATIENT_TRIAL,
//                        Tables.PATIENT_DIMENSION,
//                        Tables.I2B2,
//                        Tables.I2B2_SECURE,
                        'ts_batch.batch_job_instance cascade')
    }

    @SuppressWarnings('ChainedTest')
    private boolean isJobCompletedSuccessFully() {
        try {
            testJobCompletedSuccessfully()
            true
        } catch (AssertionError ae) {
            false
        }
    }

    @Test
    void testJobCompletedSuccessfully() {
        def execution = jobRepository.getLastJobExecution(
                ClinicalDataLoadJobConfiguration.JOB_NAME, jobParameters)

        assertThat execution.endTime, allOf(
                is(notNullValue()),
                within(30, TimeUnit.SECONDS, endImportDate)
        )

        assertThat execution.status, is(equalTo(BatchStatus.COMPLETED))
    }

    @Test
    void testCorrectNumberOfPatients() {
        assumeTrue jobCompletedSuccessFully

        long numPatientDim = rowCounter.count(
                Tables.PATIENT_DIMENSION,
                'sourcesystem_cd LIKE :pat',
                 pat: "$STUDY_ID:%")

        assertThat numPatientDim, is(NUMBER_OF_PATIENTS)

        long numPatientTrial = rowCounter.count(
                Tables.PATIENT_TRIAL,
                'trial = :trial',
                trial: STUDY_ID)

        assertThat numPatientTrial, is(NUMBER_OF_PATIENTS)
    }

    @Test
    void testAPatientIsLoadedCorrectly() {
        assumeTrue jobCompletedSuccessFully

        def q = """
            SELECT patient_num, sex_cd, age_in_years_num
            FROM ${Tables.PATIENT_DIMENSION}
            WHERE sourcesystem_cd = :ss"""
        def p = [ss: 'GSE8581:GSE8581GSM211865']

        List<Map<String, Object>> r = jdbcTemplate.queryForList q, p
        assertThat r, contains(allOf(
                hasEntry(is('patient_num'), is(notNullValue())),
                hasEntry('sex_cd', 'male'),
                hasEntry('age_in_years_num', BigDecimal.valueOf(69)),
        ))
    }

    @Test
    void testNumberOfFactsIsCorrect() {
        assumeTrue jobCompletedSuccessFully

        long numFacts = rowCounter.count(
                Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        // we delete one fact through word mapping, hence the -1
        assertThat numFacts, is(NUMBER_OF_PATIENTS * NUMBER_OF_VARIABLES - 1)
    }

    @Test
    void testCategoricalVariablesMatchConceptName() {
        assumeTrue jobCompletedSuccessFully

        def q = """
            SELECT C.concept_path, O.tval_char
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
            WHERE valtype_cd = 'T' AND O.sourcesystem_cd = :ss"""

        def r = jdbcTemplate.queryForList q, [ss: STUDY_ID]

        assertThat r, is(not(empty()))

        assertThat r, everyItem(
                new BaseMatcher<Map>() {
                    boolean matches(Object item) {
                        new ConceptFragment(item.concept_path)[-1] ==
                                item.tval_char
                    }

                    void describeTo(Description description) {
                        description.appendText("a map whose last element of " +
                                "the \\-separated concept_path key is the " +
                                "value of the tval_char key")
                    }
                })
    }

    @Test
    void testNumericalValuesHaveCorrectTval() {
        assumeTrue jobCompletedSuccessFully

        // all the numeric values should be loaded as having a value equal
        // (as opposed to lower than, greater than and so on) to the one
        // specified in the data file. This is represented in i2b2 by using
        // 'E' in the tval_char column

        def q = """
            SELECT DISTINCT tval_char
            FROM ${Tables.OBSERVATION_FACT} O
            WHERE valtype_cd = 'N' AND sourcesystem_cd = :ss"""

        def r = jdbcTemplate.queryForList q, [ss: STUDY_ID]

        assertThat r, contains(hasEntry('tval_char', 'E'))
    }

    @Test
    void testFactsConstantColumns() {
        assumeTrue jobCompletedSuccessFully

        def q = """
            SELECT DISTINCT
                provider_id,
                modifier_cd,
                instance_num,
                valueflag_cd,
                quantity_num,
                units_cd,
                end_date,
                location_cd,
                observation_blob,
                confidence_num,
                update_date,
                download_date,
                upload_id,
                sample_cd
            FROM ${Tables.OBSERVATION_FACT}
            WHERE sourcesystem_cd = :ss"""

        def r = jdbcTemplate.queryForList q, [ss: STUDY_ID]

        assertThat r, is(not(empty()))

        assertThat r, everyItem(allOf(
                hasEntry('provider_id', '@'),
                hasEntry('instance_num', BigDecimal.valueOf(1)),
                hasEntry('valueflag_cd', '@'),
                hasEntry(is('quantity_num'), nullValue()),
                hasEntry(is('units_cd'), nullValue()),
                hasEntry(is('end_date'), nullValue()),
                hasEntry('location_cd', '@'),
                hasEntry(is('observation_blob'), nullValue()),
                hasEntry(is('confidence_num'), nullValue()),
                hasEntry(is('update_date'), nullValue()),
                hasEntry(is('download_date'), nullValue()),
                hasEntry(is('upload_id'), nullValue()),
                hasEntry(is('sample_cd'), nullValue()),
        ))
    }

    @Test
    void testFactValuesForAPatient() {
        /* GSE8581GSM211865 Homo sapiens caucasian male 69 year 67 inch 71
            lung D non-small cell squamous cell carcinoma 2.13 */

        assumeTrue jobCompletedSuccessFully

        // Test a numerical and a categorical concept
        def expected = [
                'Endpoints\\FEV1\\': 2.13,
                'Subjects\\Organism\\Homo sapiens\\': 'Homo sapiens',
        ]

        def q = """
            SELECT C.concept_path, O.tval_char, O.nval_num
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
            WHERE patient_num = (
                SELECT patient_num
                FROM ${Tables.PATIENT_DIMENSION}
                WHERE sourcesystem_cd = :patient)"""

        def r = jdbcTemplate.queryForList q, [patient: 'GSE8581:GSE8581GSM211865']

        assertThat r, hasItems(
                expected.collect { pathEnding, value ->
                    allOf(
                            hasEntry(is('concept_path'), endsWith(pathEnding)),
                            value instanceof String ?
                                    hasEntry(is('tval_char'), is(value)) :
                                    hasEntry(is('nval_num'), closeTo(value, DELTA))
                    )
                } as Matcher[]
        )
    }

    @Test
    void testI2b2AndConceptDimensionMatch() {
        assumeTrue jobCompletedSuccessFully

        long numI2b2 = rowCounter.count(
                Tables.I2B2,
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        assertThat numI2b2, is(greaterThan(0L))

        def q
        def numJoined

        // they should match through the concept path
        q = """
            SELECT COUNT(*)
            FROM ${Tables.I2B2} I
            INNER JOIN ${Tables.CONCEPT_DIMENSION} D
                ON (I.c_fullname = D.concept_path)
            WHERE I.sourcesystem_cd = :study"""
        numJoined = jdbcTemplate.queryForObject(q, [study: STUDY_ID], Long)

        assertThat numJoined, is(equalTo(numI2b2))

        // they should also match through the "basecode"
        q = """
            SELECT COUNT(*)
            FROM ${Tables.I2B2} I
            INNER JOIN ${Tables.CONCEPT_DIMENSION} D
                ON (I.c_basecode = D.concept_cd)
            WHERE I.sourcesystem_cd = :study"""
        numJoined = jdbcTemplate.queryForObject(q, [study: STUDY_ID], Long)

        assertThat numJoined, is(equalTo(numI2b2))
    }

    @Test
    void testI2b2AndI2b2SecureMatch() {
        assumeTrue jobCompletedSuccessFully

        def q = """
            SELECT *
            FROM ${Tables.I2B2} I
            WHERE sourcesystem_cd = :study
            ORDER BY c_fullname"""

        def r = jdbcTemplate.queryForList(q, [study: STUDY_ID])

        def qSec = """
            SELECT *
            FROM ${Tables.I2B2_SECURE} I
            WHERE sourcesystem_cd = :study
            ORDER BY c_fullname"""
        def rSec = jdbcTemplate.queryForList(qSec, [study: STUDY_ID])

        assertThat r, allOf(
                hasSize(greaterThan(0)),
                hasSize(rSec.size()))

        // i2b2_secure has one more column than i2b2, so we test
        // that we find all the columns in i2b2 against those in
        // i2b2_secure (with two exceptions)
        0..(r.size() - 1).each { i ->
            assertThat rSec[i], allOf(
                    r[i].collect { column, value ->
                        // exclude i2b2_id/record_id columns from comparison
                        column.equalsIgnoreCase('i2b2_id') ||
                                column.equalsIgnoreCase('record_id') ?
                                null :
                                hasEntry(column, value)
                    }.findAll()
            )
        }
    }

    @Test
    void testI2b2SecureTokensArePublic() {
        assumeTrue jobCompletedSuccessFully

        def q = """
            SELECT DISTINCT secure_obj_token
            FROM ${Tables.I2B2_SECURE} I
            WHERE sourcesystem_cd = :study"""

        def r = jdbcTemplate.queryForList(q, [study: STUDY_ID])

        assertThat r, contains(hasEntry('secure_obj_token', 'EXP:PUBLIC'))
    }

    private List modifierCodesForConceptPath(String conceptPath) {
        def q = """
            SELECT DISTINCT modifier_cd
            FROM ${Tables.OBSERVATION_FACT} O INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON O.concept_cd = C.concept_cd
            WHERE C.concept_path = :cp AND O.sourcesystem_cd = :study
        """

        jdbcTemplate.queryForList(q, [cp: conceptPath, study: STUDY_ID])
    }

    @Test
    void testMalePatientsXtrial() {
        assumeTrue jobCompletedSuccessFully

        def maleCp = '\\Public Studies\\GSE8581\\Subjects\\Sex\\male\\'
        def modifierCd = 'SNOMED:F-03CE6' // modifier code for male

        def r = modifierCodesForConceptPath maleCp

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }

    @Test
    void testCaucasianXtrial() {
        assumeTrue jobCompletedSuccessFully

        def cauc = '\\Public Studies\\GSE8581\\Subjects\\Ethnicity\\Caucasian\\'
        def modifierCd = 'DEMO:RACE:CAUC'

        def r = modifierCodesForConceptPath cauc

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }

    @Test
    void testFEV1Xtrial() {
        assumeTrue jobCompletedSuccessFully

        def fev1 = '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'
        def modifierCd = 'SNOMED:F-0320A' // modifier code for male

        def r = modifierCodesForConceptPath fev1

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }
}
