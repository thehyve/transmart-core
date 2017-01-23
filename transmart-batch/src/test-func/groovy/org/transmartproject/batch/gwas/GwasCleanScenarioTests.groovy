package org.transmartproject.batch.gwas

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TestRule
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
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Test loading of two GWAS analysis on the same job from a clean database.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class GwasCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'MAGIC'

    private final static double DELTA = 1e-12d

    private final static int NUMBER_OF_ANALYSES = 2
    private final static String ANALYSIS_2HR_GLUCOSE_NAME = 'MAGIC_2hrGlucose_AdjustedForBMI'
    private final static String ANALYSIS_FASTING_GLUCOSE_NAME = 'MAGIC_FastingGlucose'
    public final static long NUMBER_OF_SNPS_2HR_GLUCOSE = 1000L
    public final static long NUMBER_OF_SNP_FASTING_GLUCOSE = 1000L

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'gwas')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.GWAS_TABLE_LISTS + 'ts_batch.batch_job_instance')
    }

    @Test
    void testDataPointTotalCount() {
        def count = rowCounter.count Tables.BIO_ASSAY_ANALYSIS_GWAS

        assertThat count, is(equalTo(
                NUMBER_OF_SNP_FASTING_GLUCOSE + NUMBER_OF_SNPS_2HR_GLUCOSE))
    }

    @Test
    void testBioAssayAnalyses() {
        def q = """
                SELECT
                    analysis_name,
                    short_description,
                    bio_assay_data_type,
                    etl_id,
                    long_description
                FROM $Tables.BIO_ASSAY_ANALYSIS
                ORDER BY analysis_name"""

        List<Map<String, Object>> r = queryForList q, [:]

        assertThat r, hasSize(NUMBER_OF_ANALYSES)
        assertThat r, everyItem(allOf(
                hasEntry(is('bio_assay_data_type'), is('GWAS')),
                hasEntry(is('etl_id'), is('MAGIC')),
                hasEntry(is('long_description'), containsString('Data on glycaemic traits')),
        ))
        assertThat r, contains(
                hasEntry(is('analysis_name'), is(ANALYSIS_2HR_GLUCOSE_NAME)),
                hasEntry(is('analysis_name'), is(ANALYSIS_FASTING_GLUCOSE_NAME)),
        )
    }

    @Test
    void testBioExperiment() {
        def q = """
                SELECT
                    accession,
                    etl_id,
                    title,
                    bio_experiment_type
                FROM $Tables.BIO_EXPERIMENT"""
        List<Map<String, Object>> r = queryForList q, [:]

        assertThat r, contains(allOf(
                hasEntry(is('accession'), is(STUDY_ID)),
                hasEntry(is('etl_id'), is("METADATA:$STUDY_ID" as String)),
                hasEntry(is('title'), is('Metadata not available')),
                hasEntry(is('bio_experiment_type'), is('Experiment'))))
    }

    @Test
    void testNumberOfSnpsDataPointsPerAnalysis() {
        def q = """
                SELECT COUNT(*) as count, analysis_name
                FROM
                    $Tables.BIO_ASSAY_ANALYSIS_GWAS G
                    INNER JOIN $Tables.BIO_ASSAY_ANALYSIS A ON(A.bio_assay_analysis_id = G.bio_assay_analysis_id)
                GROUP BY analysis_name
                ORDER BY analysis_name"""

        List<Map<String, Object>> r = queryForList q, [:]

        assertThat r, contains(
                allOf(
                        hasEntry(is('count'), isIntegerNumber(NUMBER_OF_SNPS_2HR_GLUCOSE)),
                        hasEntry(is('analysis_name'), is(ANALYSIS_2HR_GLUCOSE_NAME)),
                ),
                allOf(
                        hasEntry(is('count'), isIntegerNumber(NUMBER_OF_SNP_FASTING_GLUCOSE)),
                        hasEntry(is('analysis_name'), is(ANALYSIS_FASTING_GLUCOSE_NAME)),
                ),
        )
    }

    @Test
    void testBioAssayAnalysesHaveCorrectCount() {
        def q = """
                SELECT
                    analysis_name,
                    data_count
                FROM $Tables.BIO_ASSAY_ANALYSIS
                ORDER BY analysis_name"""

        List<Map<String, Object>> r = queryForList q, [:]

        assertThat r, contains(
                allOf(
                        hasEntry(is('data_count'), isIntegerNumber(NUMBER_OF_SNPS_2HR_GLUCOSE)),
                        hasEntry(is('analysis_name'), is(ANALYSIS_2HR_GLUCOSE_NAME)),
                ),
                allOf(
                        hasEntry(is('data_count'), isIntegerNumber(NUMBER_OF_SNP_FASTING_GLUCOSE)),
                        hasEntry(is('analysis_name'), is(ANALYSIS_FASTING_GLUCOSE_NAME)),
                ),
        )
    }

    @Test
    void testOneDataPoint() {
        def q = """
                SELECT
                    bio_asy_analysis_gwas_id,
                    p_value_char,
                    ext_data,
                    p_value,
                    log_p_value
                FROM
                    $Tables.BIO_ASSAY_ANALYSIS_GWAS G
                    INNER JOIN $Tables.BIO_ASSAY_ANALYSIS A
                        ON (A.bio_assay_analysis_id = G.bio_assay_analysis_id)
                WHERE
                    rs_id = :rs_id
                    AND A.analysis_name = :analysis_name"""

        List<Map<String, Object>> r = queryForList(
                q, [rs_id: 'rs10', analysis_name: ANALYSIS_2HR_GLUCOSE_NAME])

        assertThat r, contains(allOf(
                hasEntry(is('bio_asy_analysis_gwas_id'), is(notNullValue())),
                hasEntry(is('p_value_char'), is('0.591')),
                hasEntry(is('ext_data'), is(';;;;;;0.033;;;;a;c;;-0.028;0.052;;;;')),
                hasEntry(is('p_value'), closeTo(0.591d, DELTA)),
                hasEntry(is('log_p_value'), closeTo(-Math.log10(0.591d), DELTA)),
        ))
    }

    @Test
    void testNumberOfRowsInTop500() {
        def count = rowCounter.count Tables.BIO_ASY_ANAL_GWAS_TOP500

        assertThat count, is(equalTo(500L * NUMBER_OF_ANALYSES))
    }

    @Test
    void testPValuesAreTheSmallerInTop500() {
        def q = """
            SELECT analysis, max(pvalue) AS pv
            FROM $Tables.BIO_ASY_ANAL_GWAS_TOP500
            GROUP BY analysis"""
        List<Map<String, Object>> r = queryForList(q, [:])
        def onTop500 = r.collectEntries { [it['analysis'], it['pv']] }

        q = """
            SELECT analysis_name, min(G.p_value) AS pv
            FROM
                $Tables.BIO_ASSAY_ANALYSIS_GWAS G
                INNER JOIN $Tables.BIO_ASSAY_ANALYSIS A
                    ON (A.bio_assay_analysis_id = G.bio_assay_analysis_id)
                LEFT JOIN $Tables.BIO_ASY_ANAL_GWAS_TOP500 T
                    ON (T.bio_assay_analysis_id = G.bio_assay_analysis_id AND T.rsid = G.rs_id)
                WHERE T.rsid IS NULL
                GROUP by analysis_name"""
        r = queryForList(q, [:])
        def notOnTop500 = r.collectEntries { [it['analysis_name'], it['pv']] }

        [ANALYSIS_2HR_GLUCOSE_NAME, ANALYSIS_FASTING_GLUCOSE_NAME].each {
            assert onTop500[it] <= notOnTop500[it]
        }
    }
}
