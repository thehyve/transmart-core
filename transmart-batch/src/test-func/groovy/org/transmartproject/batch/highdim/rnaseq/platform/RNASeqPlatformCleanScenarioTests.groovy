package org.transmartproject.batch.highdim.rnaseq.platform

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
class RnaSeqPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'RNASEQ_ANNOT'
    private final static String MARKER_TYPE = 'RNASEQ_RCNT'
    private final static Long NUMBER_OF_REGIONS = 4

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE = new RunJobRule(PLATFORM_ID, 'rnaseq_annotation')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO,
                          Tables.CHROMOSOMAL_REGION,
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
                hasEntry('title', 'Test RNASeq Platform'),
                hasEntry('organism', 'Homo Sapiens'),
                hasEntry('marker_type', MARKER_TYPE),
                hasEntry('genome_build', 'hg19'),
        )
    }

    @Test
    void testNumberOfRegions() {
        def q = """
                SELECT COUNT(*)
                FROM ${Tables.CHROMOSOMAL_REGION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID]

        def count = jdbcTemplate.queryForObject q, p, Long

        assertThat count, is(equalTo(NUMBER_OF_REGIONS))
    }

    @Test
    void testChromosomalRegionRow() {
        def q = """
                SELECT *
                FROM ${Tables.CHROMOSOMAL_REGION}
                WHERE gene_id = :gene_id
        """
        def p = [gene_id: 567]

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, contains(
                allOf(
                        hasEntry('gpl_id', PLATFORM_ID),
                        hasEntry('region_name', '_WASH7P'),
                        hasEntry('chromosome', '1'),
                        hasEntry(equalTo('start_bp'), isIntegerNumber(2)),
                        hasEntry(equalTo('end_bp'), isIntegerNumber(3)),
                        hasEntry(equalTo('num_probes'), isIntegerNumber(4)),
                        hasEntry('cytoband', 'CYT'),
                        hasEntry('gene_symbol', 'WASH7P'),
                        hasEntry('organism', 'Homo Sapiens'),
                )
        )

    }

}
