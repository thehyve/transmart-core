package org.transmartproject.batch.highdim.proteomics.platform

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
 * Test proteomics platform load from a clean state.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ProteomicsPlatformCleanScenarioTests implements JobRunningTestTrait {

    private final static String PLATFORM_ID = 'PROT_ANNOT'

    static final long NUMBER_OF_PROBES = 8

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(PLATFORM_ID, 'proteomics_annotation'),
            insertTestProteinsInBioMarkerTable()
    ])

    private static ExternalResource insertTestProteinsInBioMarkerTable() {
        new ExternalResource() {
            @Override
            protected void before() throws Throwable {
                super.before()

                def context = new AnnotationConfigApplicationContext(GenericFunctionalTestConfiguration)
                try {
                    NamedParameterJdbcTemplate jdbcTempleate = context.getBean(NamedParameterJdbcTemplate)
                    jdbcTempleate.update("INSERT INTO ${Tables.BIO_MARKER}" +
                            "(bio_marker_name, primary_external_id, bio_marker_type)" +
                            " VALUES ('0TEST_NAME', '0TEST', 'PROTEIN')",
                            [:])
                    jdbcTempleate.update("INSERT INTO ${Tables.BIO_MARKER}" +
                            "(bio_marker_name, primary_external_id, bio_marker_type)" +
                            " VALUES ('E9PC15_NAME', 'E9PC15', 'PROTEIN')",
                            [:])
                } finally {
                    context.close()
                }
            }
        }
    }
    public final static TestRule RUN_JOB_RULE = RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate([Tables.GPL_INFO,
                          Tables.PROTEOMICS_ANNOTATION,
                          'ts_batch.batch_job_instance',
                          Tables.BIO_MARKER])
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
                SELECT gpl_id, uniprot_id, uniprot_name, organism, chromosome, start_bp, end_bp
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
                        hasEntry('uniprot_name', 'E9PC15_NAME'),
                        hasEntry('organism', 'Homo Sapiens'),
                        hasEntry('chromosome', '7'),
                        hasEntry(is('start_bp'), isIntegerNumber(141251077l)),
                        hasEntry(is('end_bp'), isIntegerNumber(141354209l)),
                )
        )

    }

    @Test
    void testAnnotationRowHasUniprotNameFilledInWithUniprotId() {
        def q = """
                SELECT uniprot_id, uniprot_name
                FROM ${Tables.PROTEOMICS_ANNOTATION}
                WHERE peptide = :peptide
        """
        def p = [peptide: '2243']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, hasSize(1)

        assertThat r, contains(
                allOf(
                        hasEntry('uniprot_id', 'P34932'),
                        hasEntry('uniprot_name', 'P34932'),
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
                ProteomicsPlatformJobConfig.JOB_NAME,
                RUN_JOB_RULE.jobParameters)
        Date expectedDate = execution.startTime

        assertThat date, allOf(
                is(notNullValue()),
                is(expectedDate))
    }

}
