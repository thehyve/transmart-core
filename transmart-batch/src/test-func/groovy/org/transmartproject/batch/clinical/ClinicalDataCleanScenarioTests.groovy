package org.transmartproject.batch.clinical

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
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
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.batch.matchers.IsInteger.isIntegerNumber

/**
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ClinicalDataCleanScenarioTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'
    public static final String STUDY_BASE_FOLDER = '\\Public Studies\\GSE8581\\'

    public static final NUMBER_OF_PATIENTS = 58L

    public static final NUMBER_OF_CATEGORICAL_VARIABLES = 5L
    public static final NUMBER_OF_NUMERICAL_VARIABLES = 4L
    public static final NUMBER_OF_VARIABLES =
            NUMBER_OF_CATEGORICAL_VARIABLES + NUMBER_OF_NUMERICAL_VARIABLES

    private static final BigDecimal DELTA = 0.005

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'clinical')

    @AfterClass
    static void cleanDatabase() {
        // TODO: implement backout study and call it here
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testCorrectNumberOfPatients() {
        long numPatientDim = rowCounter.count(
                Tables.PATIENT_DIMENSION,
                'sourcesystem_cd LIKE :pat',
                 pat: "$STUDY_ID:%")

        assertThat numPatientDim, is(NUMBER_OF_PATIENTS)

        long numPatientTrial = rowCounter.count(
                Tables.PATIENT_TRIAL,
                'trial = :trial',
                trial: STUDY_ID)

        assertThat numPatientTrial, is(NUMBER_OF_PATIENTS)
    }

    @Test
    void testAPatientIsLoadedCorrectly() {
        def q = """
            SELECT patient_num, sex_cd, age_in_years_num
            FROM ${Tables.PATIENT_DIMENSION}
            WHERE sourcesystem_cd = :ss"""
        def p = [ss: 'GSE8581:GSE8581GSM211865']

        List<Map<String, Object>> r = queryForList q, p
        assertThat r, contains(allOf(
                hasEntry(is('patient_num'), is(notNullValue())),
                hasEntry('sex_cd', 'male'),
                hasEntry('age_in_years_num', BigDecimal.valueOf(69)),
        ))
    }

    @Test
    void testNumberOfFactsIsCorrect() {
        long numFacts = rowCounter.count(
                Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        // we delete one fact through word mapping, hence the -1
        assertThat numFacts, is(NUMBER_OF_PATIENTS * NUMBER_OF_VARIABLES - 1)
    }

    @Test
    void testCategoricalVariablesMatchConceptName() {
        def q = """
            SELECT C.concept_path, O.tval_char
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
            WHERE valtype_cd = 'T' AND O.sourcesystem_cd = :ss"""

        def r = queryForList q, [ss: STUDY_ID]

        assertThat r, is(not(empty()))

        assertThat r, everyItem(
                new BaseMatcher<Map>() {
                    boolean matches(Object item) {
                        new ConceptFragment(item.concept_path)[-1] ==
                                item.tval_char
                    }

                    void describeTo(Description description) {
                        description.appendText("a map whose last element of " +
                                "the \\-separated concept_path key is the " +
                                "value of the tval_char key")
                    }
                })
    }

    @Test
    void testNumericalValuesHaveCorrectTval() {
        // all the numeric values should be loaded as having a value equal
        // (as opposed to lower than, greater than and so on) to the one
        // specified in the data file. This is represented in i2b2 by using
        // 'E' in the tval_char column

        def q = """
            SELECT DISTINCT tval_char
            FROM ${Tables.OBSERVATION_FACT} O
            WHERE valtype_cd = 'N' AND sourcesystem_cd = :ss"""

        def r = queryForList q, [ss: STUDY_ID]

        assertThat r, contains(hasEntry('tval_char', 'E'))
    }

    @Test
    void testFactsConstantColumns() {
        def q = """
            SELECT
                provider_id,
                modifier_cd,
                instance_num,
                valueflag_cd,
                quantity_num,
                units_cd,
                end_date,
                location_cd,
                observation_blob,
                confidence_num,
                update_date,
                download_date,
                upload_id,
                sample_cd
            FROM ${Tables.OBSERVATION_FACT}
            WHERE sourcesystem_cd = :ss"""

        def r = queryForList q, [ss: STUDY_ID]

        assertThat r, is(not(empty()))

        assertThat r, everyItem(allOf(
                hasEntry('provider_id', '@'),
                hasEntry('instance_num', BigDecimal.valueOf(1)),
                hasEntry('valueflag_cd', '@'),
                hasEntry(is('quantity_num'), nullValue()),
                hasEntry(is('units_cd'), nullValue()),
                hasEntry(is('end_date'), nullValue()),
                hasEntry('location_cd', '@'),
                hasEntry(is('observation_blob'), nullValue()),
                hasEntry(is('confidence_num'), nullValue()),
                hasEntry(is('update_date'), nullValue()),
                hasEntry(is('download_date'), nullValue()),
                hasEntry(is('upload_id'), nullValue()),
                hasEntry(is('sample_cd'), nullValue()),
        ))
    }

    // maps have keys concept_path, tval_char and nval_num
    private List<Map<String, ?>> factsForPatient(String patient) {
        def q = """
            SELECT C.concept_path, O.tval_char, O.nval_num
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
            WHERE patient_num = (
                SELECT patient_num
                FROM ${Tables.PATIENT_DIMENSION}
                WHERE sourcesystem_cd = :patient)"""

        queryForList q, [patient: "GSE8581:$patient"]
    }

    @Test
    void testFactValuesForAPatient() {
        /* GSE8581GSM211865 Homo sapiens caucasian male 69 year 67 inch 71
            lung D non-small cell squamous cell carcinoma 2.13 */

        // Test a numerical and a categorical concept
        def expected = [
                'Endpoints\\FEV1\\': 2.13,
                'Subjects\\Organism\\Homo sapiens\\': 'Homo sapiens',
        ]

        def r = factsForPatient('GSE8581GSM211865')

        assertThat r, hasItems(
                expected.collect { pathEnding, value ->
                    allOf(
                            hasEntry(is('concept_path'), endsWith(pathEnding)),
                            value instanceof String ?
                                    hasEntry(is('tval_char'), is(value)) :
                                    hasEntry(is('nval_num'), closeTo(value, DELTA))
                    )
                } as Matcher[]
        )
    }

    @Test
    void testI2b2AndConceptDimensionMatch() {
        long numI2b2 = rowCounter.count(
                Tables.I2B2,
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        assertThat numI2b2, is(greaterThan(0L))

        def q
        def numJoined

        // they should match through the concept path
        q = """
            SELECT COUNT(*)
            FROM ${Tables.I2B2} I
            INNER JOIN ${Tables.CONCEPT_DIMENSION} D
                ON (I.c_fullname = D.concept_path)
            WHERE I.sourcesystem_cd = :study"""
        numJoined = jdbcTemplate.queryForObject(q, [study: STUDY_ID], Long)

        assertThat numJoined, is(equalTo(numI2b2))

        // they should also match through the "basecode"
        q = """
            SELECT COUNT(*)
            FROM ${Tables.I2B2} I
            INNER JOIN ${Tables.CONCEPT_DIMENSION} D
                ON (I.c_basecode = D.concept_cd)
            WHERE I.sourcesystem_cd = :study"""
        numJoined = jdbcTemplate.queryForObject(q, [study: STUDY_ID], Long)

        assertThat numJoined, is(equalTo(numI2b2))
    }

    @Test
    void testI2b2AndI2b2SecureMatch() {
        def q = """
            SELECT *
            FROM ${Tables.I2B2} I
            WHERE sourcesystem_cd = :study
            ORDER BY c_fullname"""

        def r = queryForList(q, [study: STUDY_ID])

        def qSec = """
            SELECT *
            FROM ${Tables.I2B2_SECURE} I
            WHERE sourcesystem_cd = :study
            ORDER BY c_fullname"""
        def rSec = queryForList(qSec, [study: STUDY_ID])

        assertThat r, allOf(
                hasSize(greaterThan(0)),
                hasSize(rSec.size()))

        // i2b2_secure has one more column than i2b2, so we test
        // that we find all the columns in i2b2 against those in
        // i2b2_secure (with two exceptions)
        0..(r.size() - 1).each { i ->
            assertThat rSec[i], allOf(
                    r[i].collect { column, value ->
                        // exclude i2b2_id/record_id columns from comparison
                        column.equalsIgnoreCase('i2b2_id') ||
                                column.equalsIgnoreCase('record_id') ?
                                null :
                                hasEntry(column, value)
                    }.findAll()
            )
        }
    }

    @Test
    void testI2b2SecureTokensArePublic() {
        def q = """
            SELECT DISTINCT secure_obj_token
            FROM ${Tables.I2B2_SECURE} I
            WHERE sourcesystem_cd = :study"""

        def r = queryForList(q, [study: STUDY_ID])

        assertThat r, contains(hasEntry('secure_obj_token', 'EXP:PUBLIC'))
    }

    private List modifierCodesForConceptPath(String conceptPath) {
        def q = """
            SELECT DISTINCT modifier_cd
            FROM ${Tables.OBSERVATION_FACT} O INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON O.concept_cd = C.concept_cd
            WHERE C.concept_path = :cp AND O.sourcesystem_cd = :study
        """

        queryForList(q, [cp: conceptPath, study: STUDY_ID])
    }

    @Test
    void testMalePatientsXtrial() {
        def maleCp = '\\Public Studies\\GSE8581\\Subjects\\Sex\\male\\'
        def modifierCd = 'SNOMED:F-03CE6' // modifier code for male

        def r = modifierCodesForConceptPath maleCp

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }

    @Test
    void testCaucasianXtrial() {
        def cauc = '\\Public Studies\\GSE8581\\Subjects\\Ethnicity\\Caucasian\\'
        def modifierCd = 'DEMO:RACE:CAUC'

        def r = modifierCodesForConceptPath cauc

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }

    @Test
    void testFEV1Xtrial() {
        def fev1 = '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'
        def modifierCd = 'SNOMED:F-0320A' // modifier code for male

        def r = modifierCodesForConceptPath fev1

        assertThat r, contains(hasEntry('modifier_cd', modifierCd))
    }

    @Test
    void testTagsAreLoaded() {
        def r = queryForList("SELECT * FROM ${Tables.I2B2_TAGS}".toString(), [:])
        assertThat r, containsInAnyOrder(
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\'),
                        hasEntry('tag', 'Human Chronic Obstructive Pulmonary Disorder (COPD) Biomarker'),
                        hasEntry('tag_type', 'TITLE'),
                        hasEntry(is('tags_idx'), isIntegerNumber(2)),
                ),
                allOf(
                        hasEntry('path', '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\'),
                        hasEntry('tag', 'http://en.wikipedia.org/wiki/FEV1/FVC_ratio'),
                        hasEntry('tag_type', 'WEB_REF'),
                        hasEntry(is('tags_idx'), isIntegerNumber(3)),
                ),
        )
    }

    @Test
    void testUnderscoresAreReplacedWithSpaces() {
        // if the column mapping file has underscores in the category_cd column,
        // it should be replaced with spaces in the full name and name
        def q = """
            SELECT c_fullname, c_name
            FROM ${Tables.I2B2} I
            WHERE sourcesystem_cd = :study"""

        def r = queryForList(q, [study: STUDY_ID])

        assertThat r, hasItem(allOf(
                hasEntry(is('c_fullname'), endsWith('\\Lung Disease\\')),
                hasEntry(is('c_name'), equalTo('Lung Disease'))))
    }

    @Test
    void testQuotesInDataFileAreStripped() {
        // female is quoted in the data file for this patient
        def r = factsForPatient('GSE8581GSM210193')

        assertThat r, hasItem(allOf(
                hasEntry(is('concept_path'), endsWith('\\female\\')),
                hasEntry(is('tval_char'), is('female'))))
    }

    @Test
    void testConceptCountsRefConsistence() {
        List mismatches = queryForList("""
            select cc.concept_path as counts_table, cd.concept_path as dim_table
            from ${Tables.CONCEPT_COUNTS} cc
            full outer join ${Tables.CONCEPT_DIMENSION} cd on cd.concept_path = cc.concept_path
            where cc.concept_path like :path escape '^'
            and (cc.concept_path is null or cd.concept_path is null)
            """, [path: STUDY_BASE_FOLDER + '%'])

        assertThat mismatches, hasSize(0)
    }

    @Test
    void testConceptCountsCorrectness() {
        List rows = queryForList("""
            select concept_path, patient_count
            from ${Tables.CONCEPT_COUNTS}
            where concept_path like :path escape '^'
            """, [path: STUDY_BASE_FOLDER + '%'])

        assertThat rows, allOf(
            hasItem(allOf(
                hasEntry(is('concept_path'), equalTo(STUDY_BASE_FOLDER)),
                hasEntry(is('patient_count'), isIntegerNumber(NUMBER_OF_PATIENTS))
            )),
            hasItem(allOf(
                hasEntry(is('concept_path'), endsWith("\\Endpoints\\")),
                hasEntry(is('patient_count'), isIntegerNumber(NUMBER_OF_PATIENTS))
            )),
            hasItem(allOf(
                hasEntry(is('concept_path'), endsWith("\\Endpoints\\Diagnosis\\")),
                hasEntry(is('patient_count'), isIntegerNumber(NUMBER_OF_PATIENTS))
            )),
            hasItem(allOf(
                hasEntry(is('concept_path'), endsWith("\\Endpoints\\Diagnosis\\carcinoid\\")),
                hasEntry(is('patient_count'), isIntegerNumber(3L))
            )),
        )
    }

    @Test
    void testTableAccess() {
        ConceptPath path = new ConceptPath('\\Public Studies\\')
        String name = 'Public Studies'

        Map row = queryForMap("""
            select
                c_table_cd,
                c_table_name,
                c_protected_access,
                c_hlevel,
                c_name,
                c_synonym_cd,
                c_visualattributes,
                c_facttablecolumn,
                c_dimtablename,
                c_columnname,
                c_columndatatype,
                c_operator,
                c_dimcode
            from ${Tables.TABLE_ACCESS}
            where c_fullname = :fullName
            """, [fullName: path.toString()])

        assertThat row, allOf(
                hasEntry('c_table_cd', 'Public Studies'),
                hasEntry('c_table_name', 'I2B2'),
                hasEntry('c_protected_access', 'N'),
                hasEntry('c_hlevel', new BigDecimal('0')),
                hasEntry('c_name', name),
                hasEntry('c_synonym_cd', 'N'),
                hasEntry('c_visualattributes', 'CAE'),
                hasEntry('c_facttablecolumn', 'concept_cd'),
                hasEntry('c_dimtablename', 'concept_dimension'),
                hasEntry('c_columnname', 'concept_path'),
                hasEntry('c_columndatatype', 'T'),
                hasEntry('c_operator', 'LIKE'),
                hasEntry('c_dimcode', path.toString()),
        )
    }

    @Test
    void testTopNodeHasStudyVisualAttribute() {
        def q = """
            SELECT c_visualattributes
            FROM ${Tables.I2B2} I
            WHERE c_fullname = :topnode"""

        def r = jdbcTemplate.queryForObject(q, [topnode: STUDY_BASE_FOLDER], String)

        assertThat r, is(equalTo('FAS'))
    }
}
