package org.transmartproject.batch.tag

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
import org.transmartproject.batch.startup.RunJob
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TagsLoadJobTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    private final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'tags')

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            RUN_JOB_RULE,
            new RunJobRule('GSE8581_simple', 'clinical'),
    ])

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    def fetchAllTags() {
        queryForList("""SELECT * FROM ${Tables.I2B2_TAGS}
                        ORDER BY tags_idx, tag_type""".toString(), [:])
    }

    @Test
    void testTagsAreLoaded() {
        assertThat fetchAllTags(), contains(
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Human Chronic Obstructive Pulmonary Disorder Biomarker'),
                        hasEntry('tag_type', 'TITLE'),
                        hasEntry(is('tags_idx'), isIntegerNumber(2)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Homo sapiens'),
                        hasEntry('tag_type', 'ORGANISM'),
                        hasEntry(is('tags_idx'), isIntegerNumber(3)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'FEV1/FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry(is('tags_idx'), isIntegerNumber(4)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'Tiffeneau-Pinelli'),
                        hasEntry('tag_type', 'SYNONYMS'),
                        hasEntry(is('tags_idx'), isIntegerNumber(5)),
                ),
        )

        def runJob = RunJob.createInstance(
                '-p', 'studies/' + STUDY_ID + '/tags.params', '-d', 'TAGS_FILE=tags_2.txt')
        runJob.run()

        assertThat fetchAllTags(), contains(
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Human Chronic Obstructive Pulmonary Disorder Biomarker'),
                        hasEntry('tag_type', 'TITLE'),
                        hasEntry(is('tags_idx'), isIntegerNumber(2)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'GSE8581'),
                        hasEntry('tag_type', 'CODE'),
                        hasEntry(is('tags_idx'), isIntegerNumber(3)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Homo sapiens'),
                        hasEntry('tag_type', 'ORGANISM'),
                        hasEntry(is('tags_idx'), isIntegerNumber(3)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry(is('tags_idx'), isIntegerNumber(4)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'Tiffeneau-Pinelli'),
                        hasEntry('tag_type', 'SYNONYMS'),
                        hasEntry(is('tags_idx'), isIntegerNumber(5)),
                ),
        )
    }
}
