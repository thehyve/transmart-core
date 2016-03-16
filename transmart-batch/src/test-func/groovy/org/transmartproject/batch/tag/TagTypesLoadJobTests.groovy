package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.batch.core.JobExecution
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.startup.RunJob
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Test loading of I2b2 tag types and loading of tags for preloaded tag types.
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TagTypesLoadJobTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    private final static TestRule RUN_JOB_RULE = new RunJobRule('global/tagtypes.params')

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            RUN_JOB_RULE,
            new RunJobRule('GSE8581_simple', 'clinical'),
    ])

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    JobExecution runJob(String... arguments) {
        def runJob = RunJob.createInstance(*arguments)

        runJob.run()

        jobRepository.getLastJobExecution(
                runJob.jobName, runJob.finalJobParameters)
    }

    def fetchAllTagTypes() {
        queryForList("""SELECT * FROM ${Tables.I2B2_TAG_TYPES}
                        ORDER BY "index", tag_type""".toString(), [:])
    }

    def fetchAllTagOptions() {
        def rows = queryForList("""SELECT * FROM ${Tables.I2B2_TAG_OPTIONS} o
                        JOIN ${Tables.I2B2_TAG_TYPES} t
                        ON o.tag_type_id = t.tag_type_id
                        ORDER BY t."index", t.tag_type, o.value""".toString(), [:])
        def types = rows*.tag_type as Set
        def options = types.collectEntries { type -> [(type): (rows.findAll { it.tag_type == type})*.value ]}
        options
    }

    def fetchAllTagValues() {
        queryForList("""SELECT * FROM ${Tables.I2B2_TAGS} t
                        JOIN ${Tables.I2B2_TAG_OPTIONS} o
                        ON t.tag_option_id = o.tag_option_id
                        ORDER BY t.tag_type, o.value""".toString(), [:])
    }

    @Test
    void testTagTypesAreLoaded() {
        runJob('-p', 'global/tagtypes.params', '-n')
        assertThat fetchAllTagTypes(), contains(
            allOf(
                hasEntry('tag_type', 'Test tag'),
                hasEntry(is('index'), isIntegerNumber(1)),
            ),
            allOf(
                    hasEntry('tag_type', 'Programming language'),
                    hasEntry(is('index'), isIntegerNumber(2)),
            ),
        )
    }

    @Test
    void testTagOptionsAreLoaded() {
        runJob('-p', 'global/tagtypes.params', '-n')
        assertThat fetchAllTagOptions(),
            allOf(
                hasEntry(
                        is('Test tag'),
                        contains('Test option 1', 'Test option 2', 'Test option 3')
                ),
                hasEntry(
                        is('Programming language'),
                        contains('C', 'Haskell', 'Java', 'Javascript', 'Pascal', 'Python', 'R')
                ),
            )
    }

    @Test
    void testLoadValidTagOption() {
        runJob('-p', 'global/tagtypes.params', '-n')
        runJob('-p', 'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=tags_3.txt', '-n')
        assertThat fetchAllTagValues(), contains(
            allOf(
                hasEntry('tag_type', 'Test tag'),
                hasEntry('value', 'Test option 1'),
                hasEntry(is('tags_idx'), isIntegerNumber(0)),
            )
        )
    }

    @Test
    void testLoadInValidTagTypeShownIfEmpty() {
        def execution = runJob('-p',
                'global/tagtypes.params',
                '-d', 'TAG_TYPES_FILE=corruption/tagtypes_invalid_shown_if_empty.tsv')
        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription', containsString("Invalid input for field 'shown_if_empty'"))
        )
    }

    @Test
    void testTagTypesDeleteType() {
        runJob('-p', 'global/tagtypes.params', '-n')
        def execution = runJob('-p',
                'global/tagtypes.params',
                '-d', 'TAG_TYPES_FILE=tagtypes_delete_type.tsv')
        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('COMPLETED')),
        )
        assertThat fetchAllTagTypes(), hasItems(
                allOf(
                        hasEntry('tag_type', 'Test tag'),
                ),
        )
        assertThat fetchAllTagValues(), contains(
                allOf(
                        hasEntry('tag_type', 'Test tag'),
                        hasEntry('value', 'Test option 1'),
                )
        )
    }

    @Test
    void testTagTypesUpdate() {
        // load initial set of tag types
        runJob('-p', 'global/tagtypes.params', '-n')
        // load tags, including a reference to a tag type option
        runJob('-p', 'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=tags_3.txt', '-n')
        // loading new set of tag types, including the referenced one
        def execution = runJob('-p',
                'global/tagtypes.params',
                '-d', 'TAG_TYPES_FILE=tagtypes_replace.tsv')
        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('COMPLETED')),
        )
        assertThat fetchAllTagTypes(), contains(
                allOf(
                        hasEntry('tag_type', 'Test tag'),
                ),
                allOf(
                        hasEntry('tag_type', 'Programming language'),
                ),
        )
        assertThat fetchAllTagOptions(),
                allOf(
                        hasEntry(
                                is('Programming language'),
                                contains('C', 'Haskell', 'Java', 'Javascript', 'Pascal', 'Python', 'R')
                        ),
                        hasEntry(
                                is('Test tag'),
                                contains('Test option 1', 'Test option 6', 'Test option 7')
                        ),
                )
        assertThat fetchAllTagValues(), contains(
                allOf(
                        hasEntry('tag_type', 'Test tag'),
                        hasEntry('value', 'Test option 1'),
                )
        )
    }

    @Test
    void testInvalidDeleteReferencedOption() {
        // load initial set of tag types
        runJob('-p', 'global/tagtypes.params', '-n')
        // load tags, including a reference to a tag type option
        runJob('-p', 'studies/' + STUDY_ID + '/tags.params',
                '-d', 'TAGS_FILE=tags_3.txt', '-n')
        // loading new set of tag types, excluding the referenced one
        def execution = runJob('-p',
                'global/tagtypes.params',
                '-d', 'TAG_TYPES_FILE=corruption/tagtypes_dangling_value.tsv')
        assertThat execution.exitStatus, allOf(
                hasProperty('exitCode', is('FAILED')),
                hasProperty('exitDescription', containsString(
                        "Cannot delete options from tag type 'Test tag', " +
                        "because of existing references: Test option 1"))
        )
    }

}
