package org.transmartproject.batch.highdim.mrna.data

import org.hamcrest.Matchers
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
import static org.transmartproject.batch.matchers.AcceptAnyNumberIsCloseTo.castingCloseTo
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * test new way of mRNA data loading with observations/modifiers that high dimensional data need in 17.1
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaDataFactRowsTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'NANONLY'
    private final static String PLATFORM_ID = 'GENE-EXPRESSION-1'

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'expression'),
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule("${STUDY_ID}", 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.MRNA_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfObservationFacts() {
        def count = rowCounter.count Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :sourcesystem_cd',
                sourcesystem_cd: STUDY_ID
        assert count == 45
    }

}
