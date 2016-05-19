package org.transmartproject.batch.highdim.cnv.data

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
import static org.transmartproject.batch.matchers.AcceptAnyNumberIsCloseTo.castingCloseTo
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * test CNV data first load
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class CnvDataCleanScenarioTests implements JobRunningTestTrait {

    private final static String STUDY_ID = 'CLUC'
    private final static String PLATFORM_ID = 'CNV_ANNOT'

    private final static long NUMBER_OF_ASSAYS = 3
    private final static long NUMBER_OF_REGIONS = 4

    private final static double DELTA = 1.0E-12d

    @ClassRule
    public final static TestRule RUN_JOB_RULES = new RuleChain([
            new RunJobRule(STUDY_ID, 'cnv'),
            new RunJobRule(PLATFORM_ID, 'cnv_annotation'),
            new RunJobRule(STUDY_ID, 'clinical'),
    ])

    // needed by the trait
    public final static TestRule RUN_JOB_RULE =
            RUN_JOB_RULES.rulesStartingWithInnerMost[0]

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + TableLists.CNV_TABLES + 'ts_batch.batch_job_instance',)
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
        def count = rowCounter.count Tables.CNV_DATA,
                'trial_name = :study_id',
                study_id: STUDY_ID

        assertThat count,
                is(equalTo(NUMBER_OF_ASSAYS * NUMBER_OF_REGIONS))
    }

    @Test
    void testArbitrarySample() {
        def sampleCode = 'CNV.COLO205'
        def subjectId = 'COLO205'

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
                hasEntry('cd_concept_path', '\\Public Studies\\CLUC\\Test\\Cnv\\cgh\\data\\'),
                hasEntry(is('assay_id'), isA(Number)),
                hasEntry('tissue_type', 'Colon'),
                hasEntry('timepoint', 't1'),
                hasEntry('trial_name', STUDY_ID),
                hasEntry('sample_type', 'cgh'),
                hasEntry('gpl_id', PLATFORM_ID),
        )
    }

    @Test
    void testArbitraryFact() {
        def q = """
                SELECT
                    D.patient_id,
                    D.flag,
                    D.chip,
                    D.segmented,
                    D.probhomloss,
                    D.probloss,
                    D.probnorm,
                    D.probgain,
                    D.probamp
                FROM
                    ${Tables.CNV_DATA} D
                    INNER JOIN ${Tables.SUBJ_SAMPLE_MAP} S ON (D.assay_id = S.assay_id)
                    INNER JOIN ${Tables.CHROMOSOMAL_REGION} A ON (D.region_id = A.region_id)
                WHERE
                    S.sample_cd = :sample_name
                    AND A.region_name = :reg_name
                    AND D.trial_name = :study_id"""

        def p = [study_id   : STUDY_ID,
                 sample_name: 'CNV.DLD1',
                 reg_name   : 'FAM138F']

        Map<String, Object> r = queryForMap q, p
        assertThat r, allOf(
                hasEntry(equalTo('patient_id'), notNullValue()),
                hasEntry(equalTo('flag'), isIntegerNumber(-1)),
                hasEntry(equalTo('chip'), castingCloseTo(-0.143d, DELTA)),
                hasEntry(equalTo('segmented'), castingCloseTo(-0.144d, DELTA)),
                hasEntry(equalTo('probhomloss'), castingCloseTo(0.002d, DELTA)),
                hasEntry(equalTo('probloss'), castingCloseTo(0.88d, DELTA)),
                hasEntry(equalTo('probnorm'), castingCloseTo(0.112d, DELTA)),
                hasEntry(equalTo('probgain'), castingCloseTo(0.005d, DELTA)),
                hasEntry(equalTo('probamp'), castingCloseTo(0.001d, DELTA)),
        )
    }

}
