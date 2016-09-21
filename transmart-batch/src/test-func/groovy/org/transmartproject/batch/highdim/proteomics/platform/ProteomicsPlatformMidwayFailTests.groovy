package org.transmartproject.batch.highdim.proteomics.platform

import com.google.common.io.Files
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.junit.FileCorruptingTestTrait

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

/**
 * Test a failure midway the loading the procedure, and then restart the job
 * with the problem fixed
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ProteomicsPlatformMidwayFailTests implements FileCorruptingTestTrait {

    private final static String PLATFORM_ID = 'PROT_ANNOT'

    @Autowired
    RowCounter rowCounter

    File originalFile = new File("studies/${PLATFORM_ID}/proteomics_annotation/proteomics_annotation.tsv")

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO,
                         Tables.PROTEOMICS_ANNOTATION,
                        'ts_batch.batch_job_instance'])
    }

    @Test
    void test() {
        // reduce the chunk size to 2
        ProteomicsPlatformStepsConfig.chunkSize = 2

        // copy data and corrupt it
        File dataFile =
                corruptFile(originalFile, 3, 2, 'Homo Sapiens BAD DATA')

        // first execution
        def params = [
                '-p', 'studies/' + PLATFORM_ID + '/proteomics_annotation.params',
                '-d', 'ANNOTATIONS_FILE=' + dataFile.absolutePath]
        firstExecution(params)

        // second execution
        Files.copy(originalFile, dataFile)
        secondExecution(params)

        // check that we have the correct number of rows
        def count = rowCounter.count Tables.PROTEOMICS_ANNOTATION, 'gpl_id = :gpl_id',
                gpl_id: PLATFORM_ID

        assertThat count, is(equalTo(
                ProteomicsPlatformCleanScenarioTests.NUMBER_OF_PROBES))
    }
}
