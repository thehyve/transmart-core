package org.transmartproject.batch.tag

import org.junit.After
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.batch.core.JobExecution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.startup.RunJob
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test that the appropriate error is raised when the tags input file does not
 * have the correct number of columns
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TagsInputFileInvalidTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    @ClassRule
    public final static TestRule RUN_JOB_RULE =
            new RunJobRule("${STUDY_ID}_simple", 'clinical')

    /**
     * On Oracle, this is a VARCHAR(2500), which with a UTF-16 encoding
     * will result in only with 1250 max code units.
     */
    private static final int MAX_EXIT_DESCRIPTION_ON_UTF16 = 1250

    @Autowired
    TableTruncator truncator

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
        assertThat execution.stepExecutions.last(),
                hasProperty('stepName', equalTo('tagsLoadStep'))
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

        assertThat execution.stepExecutions.last(),
                hasProperty('stepName', equalTo('tagsLoadStep'))
    }

    @Test
    void testValuesTooLong() {
        def execution = runJob('-p',
                'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=corruption/tags_1_too_long.txt')

        if (execution.exitStatus.exitDescription.length() == MAX_EXIT_DESCRIPTION_ON_UTF16) {
            assertThat execution.exitStatus, allOf(
                    hasProperty('exitCode', is('FAILED')),
                    hasProperty('exitDescription',
                            containsString('Validation failed for org.transmartproject.batch.tag.Tag')))
        } else {
            assertThat execution.exitStatus, allOf(
                    hasProperty('exitCode', is('FAILED')),
                    hasProperty('exitDescription', allOf(
                            containsString('Field "tagTitle" has size 401, which exceeds the maximum size 400'),
                            containsString('Field "tagDescription" has size 1,029, ' +
                                    'which exceeds the maximum size 1,000'),
                    )))
        }
    }

    @Test
    void testDuplicates() {
        def execution = runJob('-p',
                'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=corruption/tags_1_repeated.txt')

        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription',
                        containsString("(key '[\\, ORGANISM]') on line 5 was first seen on line 2")))
    }

    @Test
    void testSkipsDueToNoConcept() {
        def execution = runJob('-p',
                "studies/${STUDY_ID}/tags.params",
                '-d', 'TAGS_FILE=corruption/tags_1_non_matching.txt')

        assertThat execution.exitStatus, hasProperty('exitCode', is('COMPLETED'))

        assertThat execution.stepExecutions, hasItem(allOf(
                hasProperty('stepName', is('tagsLoadStep')),
                hasProperty('processSkipCount', is(1))
        ))
    }


    @After
    void cleanBatchTables() {
        truncator.truncate(Tables.I2B2_TAGS)
    }

    @AfterClass
    static void afterClass() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }
}
