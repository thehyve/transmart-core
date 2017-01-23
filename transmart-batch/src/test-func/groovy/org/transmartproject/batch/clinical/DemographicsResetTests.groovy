package org.transmartproject.batch.clinical

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
import static org.hamcrest.Matchers.*

/**
 * Tests that patient demographics get reset when clinical file does not contain demographics columns
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class DemographicsResetTests implements JobRunningTestTrait {

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RuleChain([
            new RunJobRule('CLUC_wo_demographics', 'clinical', ['-d', 'STUDY_ID=CLUC', '-n']),
            new RunJobRule('CLUC', 'clinical'),
    ])

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testPatientDemographicsReset() {
        List<Map<String, Object>> r =
                queryForList("""
                    SELECT sourcesystem_cd, race_cd, sex_cd, age_in_years_num
                    FROM ${Tables.PATIENT_DIMENSION}
                    WHERE sourcesystem_cd in (:cd)""", [cd: ['CLUC:CACO2', 'CLUC:SW1398']])

        assertThat r, containsInAnyOrder(
                allOf(
                        hasEntry('sourcesystem_cd', 'CLUC:CACO2'),
                        hasEntry('race_cd', null),
                        hasEntry('sex_cd', null),
                        hasEntry('age_in_years_num', null),
                ),
                allOf(
                        hasEntry('sourcesystem_cd', 'CLUC:SW1398'),
                        hasEntry('race_cd', null),
                        hasEntry('sex_cd', null),
                        hasEntry('age_in_years_num', null),
                ),
        )
    }

}
