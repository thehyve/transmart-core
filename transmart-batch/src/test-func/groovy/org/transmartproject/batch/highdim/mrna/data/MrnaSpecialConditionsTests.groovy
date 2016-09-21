package org.transmartproject.batch.highdim.mrna.data

import org.junit.After
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.FileCorruptingTestTrait
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.startup.RunJob
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

/**
 * Test misc aspects of the mrna job.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaSpecialConditionsTests
        implements JobRunningTestTrait, FileCorruptingTestTrait {

    private final static String STUDY_ID = 'GSE8581'
    private final static String PLATFORM_ID = 'GPL570_bogus'

    @Autowired
    TableTruncator truncator

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'mrna_annotation'),
            new RunJobRule("${STUDY_ID}_simple", 'clinical'),
    ])

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + [Tables.MRNA_ANNOTATION,
                        Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @After
    void cleanData() {
        truncator.truncate([Tables.MRNA_DATA, Tables.SUBJ_SAMPLE_MAP])
    }

    File originalFile = new File('studies/GSE8581/expression/GSE8581_series_matrix.txt')

    @Test
    void testAllowMissingAnnotationsParameter() {
        MrnaDataStepsConfig.dataFilePassChunkSize = 2

        // remove the last element
        File dataFile = alterFile(originalFile) { List<List<String>> it ->
            it.pop()
        }

        def params = [
                '-p', 'studies/' + STUDY_ID + '/expression.params',
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath]
        def runJob = RunJob.createInstance(*params)
        def intResult = runJob.run()

        // should fail
        assertThat intResult, is(1)

        params = [
                '-p', 'studies/' + STUDY_ID + '/expression.params',
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath,
                '-d', 'ALLOW_MISSING_ANNOTATIONS=Y']
        runJob = RunJob.createInstance(*params)
        intResult = runJob.run()

        // and now succeed
        assertThat intResult, is(0)
    }

}
