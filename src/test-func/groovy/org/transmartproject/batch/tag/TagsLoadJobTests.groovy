package org.transmartproject.batch.tag

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
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
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    def getAllTags() {
        jdbcTemplate.queryForList("SELECT * FROM ${Tables.I2B2_TAGS}".toString(), [:])
    }

    @Test
    void testTagsAreLoaded() {
        assertThat allTags, containsInAnyOrder(
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Human Chronic Obstructive Pulmonary Disorder Biomarker'),
                        hasEntry('tag_type', 'TITLE'),
                        hasEntry('tags_idx', 2),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Homo sapiens'),
                        hasEntry('tag_type', 'ORGANISM'),
                        hasEntry('tags_idx', 3),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'FEV1/FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry('tags_idx', 4),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'Tiffeneau-Pinelli'),
                        hasEntry('tag_type', 'SYNONYMS'),
                        hasEntry('tags_idx', 5),
                ),
        )

        def runJob = RunJob.createInstance(
                '-p', 'studies/' + STUDY_ID + '/tags.params', '-d', 'TAGS_FILE=tags_2.txt')
        runJob.run()

        assertThat allTags, containsInAnyOrder(
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Human Chronic Obstructive Pulmonary Disorder Biomarker'),
                        hasEntry('tag_type', 'TITLE'),
                        hasEntry('tags_idx', 2),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Homo sapiens'),
                        hasEntry('tag_type', 'ORGANISM'),
                        hasEntry('tags_idx', 3),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'GSE8581'),
                        hasEntry('tag_type', 'CODE'),
                        hasEntry('tags_idx', 3),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry('tags_idx', 4),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'Tiffeneau-Pinelli'),
                        hasEntry('tag_type', 'SYNONYMS'),
                        hasEntry('tags_idx', 5),
                ),
        )
    }
}
