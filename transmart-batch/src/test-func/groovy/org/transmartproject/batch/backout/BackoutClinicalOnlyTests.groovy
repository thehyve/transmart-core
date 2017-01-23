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
import org.transmartproject.batch.clinical.ClinicalDataCleanScenarioTests
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.is
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber
import static org.transmartproject.batch.support.StringUtils.escapeForLike

/**
 * Run a backout job with INCLUDED_TYPES=clinical
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class BackoutClinicalOnlyTests  implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    private static final String PUBLIC_STUDIES_PATH = '\\Public Studies\\'
    private static final String TOP_NODE_PATH = '\\Public Studies\\GSE8581\\'

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'backout', ['-d', 'INCLUDED_TYPES=clinical']),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNoFactsRemain() {
        assertThat rowCounter.count(Tables.OBSERVATION_FACT), is(0L)
    }

    @Test
    void testTwoConceptsRemain() {
        def res = queryForList("""
                SELECT c_fullname FROM $Tables.I2B2
                ORDER BY c_fullname""", [:], String)

        assertThat res, contains(
                is(PUBLIC_STUDIES_PATH),
                is(TOP_NODE_PATH))
    }

    @Test
    void testConceptCountForTopNodeIsZero() {
        def res = queryForList("""
                SELECT patient_count
                FROM $Tables.CONCEPT_COUNTS
                WHERE concept_path LIKE :path_expr ESCAPE '\\'""",
                [path_expr: escapeForLike(PUBLIC_STUDIES_PATH) + '%'], Long)

        assertThat res, contains(isIntegerNumber(0l))
    }

    @Test
    void testPatientsRemain() {
        long numPatientDim = rowCounter.count(
                Tables.PATIENT_DIMENSION,
                'sourcesystem_cd LIKE :pat',
                pat: "$STUDY_ID:%")

        assertThat numPatientDim, is(ClinicalDataCleanScenarioTests.NUMBER_OF_PATIENTS)
    }
}
