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
 * Tests if data loading works when the data is split between two files that
 * have some patients in common.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ClinicalDataSplitDataTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    private static final long NUMBER_OF_PATIENTS = 10L

    @ClassRule
    public final static TestRule RUN_JOB_RULE =
            new RunJobRule("${STUDY_ID}_split_datafile", 'clinical', ['-d', 'STUDY_ID=' + STUDY_ID])

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfPatients() {
        long numPatientDim = rowCounter.count(
                Tables.PATIENT_DIMENSION,
                'sourcesystem_cd LIKE :pat',
                pat: "$STUDY_ID:%")

        assertThat numPatientDim, is(NUMBER_OF_PATIENTS)
    }

}
