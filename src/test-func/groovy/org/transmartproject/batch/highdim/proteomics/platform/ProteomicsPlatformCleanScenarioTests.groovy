package org.transmartproject.batch.highdim.proteomics.platform

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Test proteomics platform load from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ProteomicsPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'PROT_ANNOT'

    static final long NUMBER_OF_PROBES = 8

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE = new RunJobRule(PLATFORM_ID, 'proteomics_annotation')

    @AfterClass
    static void cleanDatabase() {
        new AnnotationConfigApplicationContext(
                GenericFunctionalTestConfiguration).getBean(TableTruncator).
                truncate([Tables.GPL_INFO,
                        Tables.PROTEOMICS_ANNOTATION,
                        'ts_batch.batch_job_instance'])
    }

    @Test
    void testGplInfoEntry() {
        def q = """
                SELECT platform, title, organism, marker_type
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform"""
        def p = [platform: PLATFORM_ID]

        Map<String, Object> r = queryForMap q, p
        assertThat r, allOf(
                hasEntry('title', 'Test Proteomics Platform'),
                hasEntry('organism', 'Homo Sapiens'),
                hasEntry('marker_type', 'PROTEOMICS'),
        )
    }

    @Test
    void testNumberOfProbes() {
        def q = """
                SELECT COUNT(*)
                FROM ${Tables.PROTEOMICS_ANNOTATION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID]

        def count = jdbcTemplate.queryForObject q, p, Long

        assertThat count, is(equalTo(NUMBER_OF_PROBES))
    }

    @Test
    void testAnnotationRow() {
        def q = """
                SELECT gpl_id, uniprot_id, organism, chromosome, start_bp, end_bp
                FROM ${Tables.PROTEOMICS_ANNOTATION}
                WHERE peptide = :peptide
        """
        def p = [peptide: '611']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, hasSize(1)

        assertThat r, contains(
                allOf(
                        hasEntry('gpl_id', 'PROT_ANNOT'),
                        hasEntry('uniprot_id', 'E9PC15'),
                        hasEntry('organism', 'Homo Sapiens'),
                        hasEntry('chromosome', '7'),
                        hasEntry(is('start_bp'), isIntegerNumber(141251077l)),
                        hasEntry(is('end_bp'), isIntegerNumber(141354209l)),
                )
        )

    }

    @Test
    void testAnnotationDate() {
        def q = """
                SELECT annotation_date organism
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform"""
        def p = [platform: PLATFORM_ID]

        Date date = jdbcTemplate.queryForObject(q, p, Date)

        def execution = jobRepository.getLastJobExecution(
                ProteomicsPlatformJobConfiguration.JOB_NAME,
                RUN_JOB_RULE.jobParameters)
        Date expectedDate = execution.startTime

        assertThat date, allOf(
                is(notNullValue()),
                is(expectedDate))
    }

}
