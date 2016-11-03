package org.transmartproject.batch.clinical

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
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
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TrialVisitWithTimeDataTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'TEST_17_1'

    public static final NUMBER_OF_PATIENTS = 8L
    public static final NUMBER_OF_FACTS = 976L
    public static final NUMBER_OF_VISITS = 7L

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'clinical')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testCorrectNumberOfPatients() {
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
    void testNumberOfFactsIsCorrect() {
        long numFacts = rowCounter.count(
                Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        assertThat numFacts, is(NUMBER_OF_FACTS)
    }

    @Test
    void testNumberOfTrialVisitsIsCorrect() {
        long numVisits = rowCounter.count([:], Tables.TRIAL_VISIT_DIMENSION)

        assertThat numVisits, is(NUMBER_OF_VISITS)
    }

}
