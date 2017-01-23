package org.transmartproject.batch.highdim.mrna.data

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
 * test mRNA data import with multiple samples
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaMultipleSamplesTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'MULTSAMPL'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    private final static long NUMBER_OF_ASSAYS = 4
    private final static long NUMBER_OF_PROBES = 19

    @Test
    void testMultipleSamplesMeasurements() {
        def params = [study_id: STUDY_ID]

        List r = queryForList """
                SELECT DISTINCT S.subject_id, S.sample_cd, P.sex_cd
                FROM
                    ${Tables.MRNA_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.PATIENT_DIMENSION} P ON (S.patient_id = P.patient_num)
                WHERE D.trial_name = :study_id""",
                params

        assertThat r, containsInAnyOrder(
                allOf(hasEntry('subject_id', 'S1'), hasEntry('sample_cd', 'S11'), hasEntry('sex_cd', 'female')),
                allOf(hasEntry('subject_id', 'S1'), hasEntry('sample_cd', 'S12'), hasEntry('sex_cd', 'female')),
                allOf(hasEntry('subject_id', 'S2'), hasEntry('sample_cd', 'S21'), hasEntry('sex_cd', 'male')),
                allOf(hasEntry('subject_id', 'S2'), hasEntry('sample_cd', 'S22'), hasEntry('sex_cd', 'male')),
        )
    }

    @Test
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.MRNA_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES))
    }

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'expression'),
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.MRNA_TABLES + 'ts_batch.batch_job_instance',)
    }

}
