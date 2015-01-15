package org.transmartproject.batch.tag

import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.startup.RunJob

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test that the appropriate error is raised when the tags input file does not
 * have the correct number of columns
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TagsInputFileInvalidTests {

    @Autowired
    JobRepository jobRepository

    public static final String STUDY_ID = 'GSE8581'

    JobExecution runJob(String... arguments) {
        def runJob = RunJob.createInstance(*arguments)

        runJob.run()

        jobRepository.getLastJobExecution(
                runJob.jobName, runJob.finalJobParameters)
    }

    @Test
    void testMissingColumns() {
        def execution = runJob('-p',
                'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=corruption/tags_1_missing_column.txt')

        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription', allOf(
                        containsString('FlatFileParseException'),
                        containsString('line: 2'),
                )))

        // if failed at the correct spot
        assertThat execution.stepExecutions, contains(
                hasProperty('stepName', equalTo('tagsLoadStep')))
    }

    @Test
    void testExtraColumns() {
        def execution = runJob('-p',
                'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=corruption/tags_1_extra_column.txt')


        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription', allOf(
                        containsString('FlatFileParseException'),
                        containsString('line: 2'),
                )))

        assertThat execution.stepExecutions, contains(
                hasProperty('stepName', equalTo('tagsLoadStep')))
    }

    @Test
    void testValuesTooLong() {
        def execution = runJob('-p',
                'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=corruption/tags_1_too_long.txt')


        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription', allOf(
                        containsString('Field "tagTitle" has size 401, which exceeds the maximum size 400'),
                        containsString('Field "tagDescription" has size 1,029, which exceeds the maximum size 1,000'),
                )))
    }


    @AfterClass
    static void cleanBatchTables() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate('ts_batch.batch_job_instance CASCADE')
    }
}
