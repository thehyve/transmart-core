package org.transmartproject.batch.tag

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.junit.SkipIfJobFailedRule
import org.transmartproject.batch.startup.RunJob

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TagsLoadJobTests {

    static JobParameters jobParameters

    public static final String STUDY_ID = 'GSE8581'

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'tags')

    @Autowired
    JobRepository jobRepository

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobFailedRule = new SkipIfJobFailedRule(
            jobRepositoryProvider: { -> jobRepository },
            runJobRule: RUN_JOB_RULE,
            jobName: TagsLoadJobConfiguration.JOB_NAME)

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate(Tables.I2B2_TAGS, 'ts_batch.batch_job_instance cascade')
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
                        hasEntry('path', '\\Public Studies\\GSE8581\\FEV1\\'),
                        hasEntry('tag', 'FEV1/FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry('tags_idx', 4),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\FEV1\\'),
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
                        hasEntry('path', '\\Public Studies\\GSE8581\\FEV1\\'),
                        hasEntry('tag', 'FVC ratio'),
                        hasEntry('tag_type', 'NAME'),
                        hasEntry('tags_idx', 4),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\FEV1\\'),
                        hasEntry('tag', 'Tiffeneau-Pinelli'),
                        hasEntry('tag_type', 'SYNONYMS'),
                        hasEntry('tags_idx', 5),
                ),
        )
    }
}
