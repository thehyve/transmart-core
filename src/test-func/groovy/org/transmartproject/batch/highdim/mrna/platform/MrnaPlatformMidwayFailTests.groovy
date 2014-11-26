package org.transmartproject.batch.highdim.mrna.platform

import com.google.common.io.Files
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.startup.RunJob

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test a failure midway the loading the procedure, and then restart the job
 * with the problem fixed
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaPlatformMidwayFailTests {

    private final static String PLATFORM_ID = 'GPL570_bogus'

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @BeforeClass
    static void beforeClass() {
        CommandLineJobRunner.presetSystemExiter({ int it -> } as SystemExiter)
    }

    @Autowired
    JobRepository repository

    @Autowired
    RowCounter rowCounter

    File originalFile = new File('studies/GPL570_bogus/annotation/GPL570_simplified.tsv')

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(
                        "${Tables.GPL_INFO} CASCADE",
                        Tables.MRNA_ANNOTATION,
                        'ts_batch.batch_job_instance CASCADE')
    }

    @Test
    void test() {
        // reduce the chunk size to 2
        MrnaPlatformJobConfiguration.chunkSize = 2

        // copy data and corrupt it
        File dataFile = temporaryFolder.newFile()
        List<String> lines = linesForFile(originalFile)
        lines[11] = lines[11][0..<-1] + '_"'
        dataFile.withWriter { w ->
            lines.each { w.write it + '\n' }
        }

        // first execution
        def runJob = RunJob.createInstance(
                '-p', 'studies/' + PLATFORM_ID + '/annotation.params',
                '-d', 'ANNOTATIONS_FILE=' + dataFile.absolutePath)
        def intResult = runJob.run()
        assertThat intResult, is(1)

        JobParameters jobParameters = runJob.finalJobParameters

        // fix the file
        Files.copy(originalFile, dataFile)

        // get last execution id
        def execution = repository.getLastJobExecution(
                MrnaPlatformJobConfiguration.JOB_NAME, jobParameters)

        // restart it
        runJob = RunJob.createInstance(
                '-p', 'studies/' + PLATFORM_ID + '/annotation.params',
                '-d', 'ANNOTATIONS_FILE=' + dataFile.absolutePath,
                '-r', '-j', execution.id as String)
        intResult = runJob.run()

        assertThat intResult, is(0)

        // check that we have the correct number of rows
        def count = rowCounter.count Tables.MRNA_ANNOTATION, 'gpl_id = :gpl_id',
                gpl_id: PLATFORM_ID

        /* + 1 because one probe has two genes */
        assertThat count, is(equalTo(
                MrnaPlatformCleanScenarioTests.NUMBER_OF_PROBES + 1L))
    }

    private List<String> linesForFile(File file) {
        def out = []
        file.eachLine { out << it }
        out
    }
}
