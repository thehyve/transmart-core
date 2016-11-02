package org.transmartproject.batch.highdim.rnaseq.transcript.platform

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Test RNASeq platform load from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class RnaSeqTranscriptPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'RNASEQ_TRANSCRIPT_ANNOT'
    private final static String MARKER_TYPE = 'RNASEQ_TRANSCRIPT'
    private final static Long NUMBER_OF_REGIONS = 3

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE = new RunJobRule(PLATFORM_ID, 'rnaseq_transcript_annotation')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO,
                          Tables.RNASEQ_TRANSCRIPT_ANNOTATION,
                          'ts_batch.batch_job_instance'])
    }

    @Test
    void testGplInfoEntry() {
        def q = """
                SELECT platform, title, organism, marker_type, genome_build
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform"""
        def p = [platform: PLATFORM_ID]

        Map<String, Object> r = queryForMap q, p
        assertThat r, allOf(
                hasEntry('title', 'Test RNASeq Transcript Platform'),
                hasEntry('organism', 'Homo Sapiens'),
                hasEntry('marker_type', MARKER_TYPE),
                hasEntry('genome_build', 'hg19'),
        )
    }

    @Test
    void testNumberOfRegions() {
        def q = """
                SELECT COUNT(*)
                FROM ${Tables.RNASEQ_TRANSCRIPT_ANNOTATION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID]

        def count = jdbcTemplate.queryForObject q, p, Long

        assertThat count, is(equalTo(NUMBER_OF_REGIONS))
    }

    @Test
    void testTranscriptAnnotationRow() {
        def q = """
                SELECT *
                FROM ${Tables.RNASEQ_TRANSCRIPT_ANNOTATION}
                WHERE transcript = :transcript
        """
        def p = [transcript: 'TR2']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, contains(
                allOf(
                        hasEntry('gpl_id', PLATFORM_ID),
                        hasEntry('ref_id', '3'),
                        hasEntry('chromosome', '10'),
                        hasEntry(equalTo('start_bp'), isIntegerNumber(3000)),
                        hasEntry(equalTo('end_bp'), isIntegerNumber(4000)),
                )
        )

    }

}
