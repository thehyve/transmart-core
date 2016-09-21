package org.transmartproject.batch.highdim.proteomics.data

import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
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

/**
 * test proteomics data import in the simplest scenario (good data not previously
 * loaded)
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ProteomicsDataCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'PROT_ANNOT'

    private final static long NUMBER_OF_ASSAYS = 16
    private final static long NUMBER_OF_PROBES = 8

    private final static BigDecimal DELTA = 1.0E-12

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'proteomics'),
            new RunJobRule(PLATFORM_ID, 'proteomics_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.PROTEOMICS_TABLES + 'ts_batch.batch_job_instance',)
    }

    @Test
    void testNumberOfRowsInSSM() {
        def count = rowCounter.count Tables.SUBJ_SAMPLE_MAP,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count, is(equalTo(NUMBER_OF_ASSAYS))
    }

    @Test
    void testNumberOfFacts() {
        def count = rowCounter.count Tables.PROTEOMICS_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_PROBES))
    }

    @Test
    void testArbitrarySample() {
        def sampleCode = 'LFQ.intensity.CACO2_2'
        def subjectId = 'CACO2'

        def q = """
                SELECT
                    PD.sourcesystem_cd as pd_sourcesystem_cd,
                    CD.concept_path as cd_concept_path,
                    assay_id,
                    sample_type,
                    trial_name,
                    tissue_type,
                    timepoint,
                    gpl_id
                FROM ${Tables.SUBJ_SAMPLE_MAP} SSM
                LEFT JOIN ${Tables.PATIENT_DIMENSION} PD ON (SSM.patient_id = PD.patient_num)
                LEFT JOIN ${Tables.CONCEPT_DIMENSION} CD ON (SSM.concept_code = CD.concept_cd)
                WHERE SSM.subject_id = :subjectId AND SSM.sample_cd = :sampleCd"""

        Map<String, Object> r = queryForMap q, [subjectId: subjectId, sampleCd: sampleCode]

        assertThat r, allOf(
                hasEntry('pd_sourcesystem_cd', "$STUDY_ID:$subjectId" as String),
                hasEntry('cd_concept_path', '\\Public Studies\\CLUC\\Molecular profiling' +
                        '\\High-throughput molecular profiling\\Expression (protein)\\LC-MS-MS\\Protein level' +
                        '\\TPNT\\MZ ratios\\'),
                hasEntry(is('assay_id'), isA(Number)),
                hasEntry('sample_type', 'LFQ-2'),
                hasEntry('trial_name', STUDY_ID),
                hasEntry('tissue_type', 'Colon'),
                hasEntry('timepoint', 'TPNT'),
                hasEntry('gpl_id', 'PROT_ANNOT'),
        )
    }

    @Test
    void testArbitraryFact() {
        def q = """
                SELECT D.subject_id, intensity,
                       log_intensity
                FROM
                    ${Tables.PROTEOMICS_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.PROTEOMICS_ANNOTATION} A ON (D.protein_annotation_id = A.id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.peptide = :ref_id
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: 'LFQ.intensity.CACO2_2',
                 ref_id     : '611']

        Map<String, Object> r = queryForMap q, p

        def value = new BigDecimal(30627000)
        def logValue = new BigDecimal(Math.log(value) / Math.log(2d))

        assertThat r, allOf(
                hasEntry('subject_id', 'CACO2'),
                hasEntry(is('intensity'), closeTo(value, DELTA)),
                hasEntry(is('log_intensity'), closeTo(logValue, DELTA)),
        )
    }

    @Test
    void testRawNANsAndNegativesAreOmitted() {
        def q = """
                SELECT *
                FROM
                    ${Tables.PROTEOMICS_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.PROTEOMICS_ANNOTATION} A ON (D.protein_annotation_id = A.id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.peptide = :ref_id
                    AND D.trial_name = :study_id
                    AND intensity != 0"""
        def p = [study_id   : STUDY_ID,
                 sample_name: 'LFQ.intensity.CACO2_1',
                 ref_id     : '5060']

        List<Map<String, Object>> r = queryForList q, p

        assertThat r, is(empty())
    }
}
