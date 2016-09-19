package org.transmartproject.batch.highdim.metabolomics.platform

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
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test metabolomics annotation loading from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class MetabolomicsPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'MET998'

    static final long NUMBER_OF_METABOLITES = 8
    static final long NUMBER_OF_SUB_PATHWAYS = 5
    static final long NUMBER_OF_SUPER_PATHWAYS = 3
    static final long NUMBER_OF_BIOCHEMICALS_WITH_SUB_PATHWAYS = 7

    @ClassRule
    public final static RunJobRule RUN_JOB_RULE =
            new RunJobRule(PLATFORM_ID, 'metabolomics_annotation')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.METABOLOMICS_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfMetabolites() {
        def count = rowCounter.count Tables.METAB_ANNOTATION,
                'gpl_id = :gpl_id', gpl_id: PLATFORM_ID

        assertThat count, is(equalTo(NUMBER_OF_METABOLITES))
    }

    @Test
    void testNumberOfSubPathways() {
        def count = rowCounter.count Tables.METAB_SUB_PATH,
                'gpl_id = :gpl_id', gpl_id: PLATFORM_ID

        assertThat count, is(equalTo(NUMBER_OF_SUB_PATHWAYS))
    }


    @Test
    void testNumberOfSuperPathways() {
        def count = rowCounter.count Tables.METAB_SUPER_PATH,
                'gpl_id = :gpl_id', gpl_id: PLATFORM_ID

        assertThat count, is(equalTo(NUMBER_OF_SUPER_PATHWAYS))
    }

    @Test
    void testCheckMetaboliteWithHmdbId() {
        def q = """
                SELECT id, hmdb_id
                FROM ${Tables.METAB_ANNOTATION}
                WHERE gpl_id = :platform AND biochemical_name = :metabolite"""
        def p = [platform: PLATFORM_ID, metabolite: 'xylitol']

        Map<String, Object> r = queryForMap q, p

        assertThat r, allOf(
                hasEntry(is('id'), isA(Number)),
                hasEntry('hmdb_id', 'HMDB00568'),)
    }

    @Test
    void testCheckMetaboliteWithoutHmdbId() {
        def q = """
                SELECT id, hmdb_id
                FROM ${Tables.METAB_ANNOTATION}
                WHERE gpl_id = :platform AND biochemical_name = :metabolite"""
        def p = [platform: PLATFORM_ID,
                 metabolite: '5-isopentenyl pyrophosphoric acid']

        Map<String, Object> r = queryForMap q, p

        assertThat r, allOf(
                hasEntry(is('id'), isA(Number)),
                hasEntry(is('hmdb_id'), is(nullValue(String))),)
    }

    @Test
    void testChainAnnotationSubSuperPathway() {
        def q = """
                SELECT sub_pathway_name, super_pathway_name
                FROM ${Tables.METAB_ANNOTATION} A
                INNER JOIN ${Tables.METAB_ANNOT_SUB} ANS ON (A.id = ANS.metabolite_id)
                INNER JOIN ${Tables.METAB_SUB_PATH} SUB ON (ANS.sub_pathway_id = SUB.id)
                INNER JOIN ${Tables.METAB_SUPER_PATH} SUP ON (SUB.super_pathway_id = SUP.id)
                WHERE A.gpl_id = :platform AND biochemical_name = :metabolite
        """
        def p = [platform: PLATFORM_ID, metabolite: 'mevalonic acid']

        Map<String, Object> r = queryForMap q, p
        assertThat r, allOf(
                hasEntry(is('sub_pathway_name'), is('Mevalonic acid pathway')),
                hasEntry(is('super_pathway_name'), is('Carboxylic acid')),)
    }
}
