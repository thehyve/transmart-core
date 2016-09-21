package org.transmartproject.batch.highdim.mrna.platform

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
import static org.springframework.context.i18n.LocaleContextHolder.locale

/**
 * Test a failure midway the loading the procedure, and then restart the job
 * with the problem fixed
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaPlatformMidwayFailTests implements FileCorruptingTestTrait {

    private final static String PLATFORM_ID = 'GPL570_bogus'
    private final static String PLATFORM_ID_NORM = 'GPL570_bogus'.toUpperCase(locale)

    @Autowired
    RowCounter rowCounter

    File originalFile = new File('studies/GPL570_bogus/GPL570_simplified.tsv')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.MRNA_ANNOTATION, Tables.GPL_INFO, 'ts_batch.batch_job_instance'])
    }

    @Test
    void test() {
        // reduce the chunk size to 2
        MrnaPlatformStepsConfig.chunkSize = 2

        // copy data and corrupt it
        File dataFile =
                corruptFile(originalFile, 11, 4, 'Homo Sapiens BAD BATA')

        // first execution
        def params = [
                '-p', 'studies/' + PLATFORM_ID + '/mrna_annotation.params',
                '-d', 'ANNOTATIONS_FILE=' + dataFile.absolutePath]
        firstExecution(params)

        // second execution
        Files.copy(originalFile, dataFile)
        secondExecution(params)

        // check that we have the correct number of rows
        def count = rowCounter.count Tables.MRNA_ANNOTATION, 'gpl_id = :gpl_id',
                gpl_id: PLATFORM_ID_NORM

        /* + 1 because one probe has two genes */
        assertThat count, is(equalTo(
                MrnaPlatformCleanScenarioTests.NUMBER_OF_PROBES + 1L))
    }
}
