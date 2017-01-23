package org.transmartproject.batch.highdim.mirna.platform

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

/**
 * Test miRNA platform load from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MirnaPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'CELL-LINE_MIRNA'

    static final long NUMBER_OF_PROBES = 30

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE = new RunJobRule(
            'studies/CLUC_shortened_mirna_annotation/mirna_annotation.params')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO, Tables.MIRNA_ANNOTATION, 'ts_batch.batch_job_instance'])
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
                hasEntry('title', 'miRNA Array'),
                hasEntry('organism', 'Homo Sapiens'),
                hasEntry('marker_type', 'MIRNA_QPCR'),
        )
    }

    @Test
    void testNumberOfRowsInMirnaAnnotation() {
        def count = rowCounter.count Tables.MIRNA_ANNOTATION, 'gpl_id = :gpl_id',
                gpl_id: PLATFORM_ID

        assertThat count, is(equalTo(NUMBER_OF_PROBES))
    }

    @Test
    void testRowsForSameAnnotation() {
        def q = """
                SELECT mirna_id, probeset_id
                FROM ${Tables.MIRNA_ANNOTATION}
                WHERE id_ref = :id_ref
        """
        def p = [id_ref: '20']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, hasSize(1)

        assertThat r, contains(
                allOf(
                        hasEntry('mirna_id', 'ebv-mir-bart10*'),
                        hasEntry(is('probeset_id'), notNullValue()),

                )
        )
    }

}
