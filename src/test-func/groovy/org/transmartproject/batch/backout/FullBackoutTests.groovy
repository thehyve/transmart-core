package org.transmartproject.batch.backout

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
import static org.hamcrest.Matchers.is

/**
 * Run a backout job (all the backout modules).
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class FullBackoutTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    private static final List<String> SECURITY_REQUIRED_PARAMS =
            ['-d', 'SECURITY_REQUIRED=Y']

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'backout', SECURITY_REQUIRED_PARAMS),
            new RunJobRule(STUDY_ID, 'clinical', SECURITY_REQUIRED_PARAMS),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    private static final String PRIVATE_STUDIES_PATH = '\\Private Studies\\'

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testOnlyPrivateStudiesRemains() {
        assertThat rowCounter.count(Tables.I2B2), is(1L)
        def q = "SELECT c_fullname FROM $Tables.I2B2"
        def r = jdbcTemplate.queryForObject(q, [:], String)
        assertThat r, is(PRIVATE_STUDIES_PATH)

        assertThat rowCounter.count(Tables.I2B2_SECURE), is(1L)
        q = "SELECT c_fullname FROM $Tables.I2B2_SECURE"
        r = jdbcTemplate.queryForObject(q, [:], String)
        assertThat r, is(PRIVATE_STUDIES_PATH)

        assertThat rowCounter.count(Tables.CONCEPT_DIMENSION), is(1L)
        q = "SELECT concept_path FROM $Tables.CONCEPT_DIMENSION"
        r = jdbcTemplate.queryForObject(q, [:], String)
        assertThat r, is(PRIVATE_STUDIES_PATH)
    }

    @Test
    void testNoFactsRemain() {
        assertThat rowCounter.count(Tables.OBSERVATION_FACT), is(0L)
    }

    @Test
    void testNoPatientsRemain() {
        assertThat rowCounter.count(Tables.PATIENT_DIMENSION), is(0L)
        assertThat rowCounter.count(Tables.PATIENT_TRIAL), is(0L)
    }

    @Test
    void testNoConceptCountsRemain() {
        assertThat rowCounter.count(Tables.CONCEPT_COUNTS), is(0L)
    }

    @Test
    void testNoTagsRemain() {
        assertThat rowCounter.count(Tables.I2B2_TAGS), is(0L)
    }

    @Test
    void testNoSecureObjectRemains() {
        def res = rowCounter.count(Tables.SECURE_OBJECT,
                'bio_data_unique_id = :id', id: "EXP:$STUDY_ID")
        assert res == 0L

        res = rowCounter.count(Tables.BIO_EXPERIMENT,
                'accession = :study', study: STUDY_ID)
        assert res == 0L

        res = rowCounter.count(Tables.BIO_DATA_UID,
                'unique_id = :study', study: STUDY_ID)
        assert res == 0L
    }
}
