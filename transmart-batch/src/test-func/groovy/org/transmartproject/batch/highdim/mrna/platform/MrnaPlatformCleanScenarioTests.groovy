package org.transmartproject.batch.highdim.mrna.platform

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
import org.transmartproject.batch.startup.RunJob

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.springframework.context.i18n.LocaleContextHolder.locale
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Test mrna platform load from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MrnaPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'GPL570_bogus'
    private final static String PLATFORM_ID_NORM = 'GPL570_bogus'.toUpperCase(locale)

    static final long NUMBER_OF_PROBES = 19

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE = new RunJobRule(PLATFORM_ID, 'mrna_annotation')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO, Tables.MRNA_ANNOTATION, 'ts_batch.batch_job_instance'])
    }

    @Test
    void testGplInfoEntry() {
        def q = """
                SELECT platform, title, organism, marker_type
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform"""
        def p = [platform: PLATFORM_ID_NORM]

        Map<String, Object> r = queryForMap q, p
        /* these are the values in GPL570_bogus/mrna_annotation.params */
        assertThat r, allOf(
                hasEntry('title', 'Affymetrix Human Genome U133A 2.0 Array'),
                hasEntry('organism', 'Homo Sapiens'),
                hasEntry('marker_type', 'Gene Expression'),
        )
    }

    @Test
    void testNumberOfRowsInMrnaAnnotation() {
        def count = rowCounter.count Tables.MRNA_ANNOTATION, 'gpl_id = :gpl_id',
                gpl_id: PLATFORM_ID_NORM

        /* + 1 because one probe has two genes */
        assertThat count, is(equalTo(NUMBER_OF_PROBES + 1L))
    }

    @Test
    void testNumberOfProbes() {
        def q = """
                SELECT COUNT(DISTINCT probe_id)
                FROM ${Tables.MRNA_ANNOTATION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID_NORM]

        def count = jdbcTemplate.queryForObject q, p, Long

        assertThat count, is(equalTo(NUMBER_OF_PROBES))
    }


    @Test
    void testRowsForSameAnnotation() {
        def q = """
                SELECT gene_symbol, probeset_id, gene_id
                FROM ${Tables.MRNA_ANNOTATION}
                WHERE probe_id = :probe
        """
        def p = [probe: '1552256_a_at']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, hasSize(2)

        /* probeset ids should be equal */
        assert r[0].probeset_id == r[1].probeset_id

        assertThat r, containsInAnyOrder(
                allOf(
                        hasEntry('gene_symbol', 'SCARB1'),
                        hasEntry(is('gene_id'), isIntegerNumber(949L))),
                allOf(
                        hasEntry('gene_symbol', 'TTLL12'),
                        hasEntry(is('gene_id'), isIntegerNumber(23170L))),

                )
    }

    @Test
    void testOrganism() {
        def q = """
                SELECT DISTINCT organism
                FROM ${Tables.MRNA_ANNOTATION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID_NORM]

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, contains(
                hasEntry('organism', 'Homo Sapiens')
        )
    }

    @Test
    void testAnnotationDate() {
        def q = """
                SELECT annotation_date organism
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform"""
        def p = [platform: PLATFORM_ID_NORM]

        Date date = jdbcTemplate.queryForObject(q, p, Date)

        def execution = jobRepository.getLastJobExecution(
                MrnaPlatformJobConfig.JOB_NAME,
                RUN_JOB_RULE.jobParameters)
        Date expectedDate = execution.startTime

        assertThat date, allOf(
                is(notNullValue()),
                is(expectedDate))
    }

    @Test
    void testOldParamsFileIsSupported() {
        def runJob = RunJob.createInstance('-p', 'studies/' + PLATFORM_ID + '/annotation.params')
        def intResult = runJob.run()

        assertThat 'second execution is successful', intResult, is(0)
        def q = """
                SELECT COUNT(DISTINCT probe_id)
                FROM ${Tables.MRNA_ANNOTATION}
                WHERE gpl_id = :platform"""
        def p = [platform: PLATFORM_ID_NORM]
        def count = jdbcTemplate.queryForObject q, p, Long
        assertThat count, is(equalTo(NUMBER_OF_PROBES))
    }

}
