package org.transmartproject.batch.highdim.cnv.data

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
import org.transmartproject.batch.highdim.mrna.data.MrnaDataStepsConfig
import org.transmartproject.batch.junit.FileCorruptingTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.startup.RunJob
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

/**
 * Test PROB_IS_NOT_1 flag
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class CnvProbIsNotOneTests implements FileCorruptingTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'CNV_ANNOT'

    File originalFile = new File("studies/${STUDY_ID}/cnv/cnv_data.tsv")

    @Autowired
    TableTruncator truncator

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'cnv_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    @AfterClass
    static void cleanDatabase() {
        MrnaDataStepsConfig.dataFilePassChunkSize = 10000
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + [Tables.CHROMOSOMAL_REGION,
                                                       Tables.CNV_DATA,
                                                       Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @Test
    void testFailOnIncorrectProbability() {
        File dataFile = corruptFile(originalFile, 2, 12, '1')

        def runJob = RunJob.createInstance('-p', "studies/${STUDY_ID}/cnv.params",
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath)
        def intResult = runJob.run()

        assertThat 'upload has failed', intResult, is(1)
    }

    @Test
    void testWarnOnIncorrectProbability() {
        File dataFile = corruptFile(originalFile, 2, 12, '1')

        def runJob = RunJob.createInstance('-p', "studies/${STUDY_ID}/cnv.params",
                '-d', 'DATA_FILE_PREFIX=' + dataFile.absolutePath, '-d', 'PROB_IS_NOT_1=WARN')
        def intResult = runJob.run()

        assertThat 'upload has finished successfully', intResult, is(0)
    }

}
