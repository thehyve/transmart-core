package org.transmartproject.batch.clinical

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

/**
 * Load clinical data for a study not loaded before.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class TrialVisitWithTimeDataTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'TEST_17_1'

    public static final NUMBER_OF_PATIENTS = 8L
    public static final NUMBER_OF_FACTS = 976L
    public static final NUMBER_OF_ANNOTATED_FACTS = 1216L - NUMBER_OF_FACTS
    public static final NUMBER_OF_VISITS = 7L

    private static final BigDecimal DELTA = 0.005

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'clinical')

    @AfterClass
    static void cleanDatabase() {
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
    void testNumberOfFactsIsCorrect() {
        long numFacts = rowCounter.count(
                Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :ss and modifier_cd = \'@\'',
                ss: STUDY_ID)

        assertThat numFacts, is(NUMBER_OF_FACTS)
    }

    @Test
    void testNumberOfAnnotatedFactsIsCorrect() {
        long numFacts = rowCounter.count(
                Tables.OBSERVATION_FACT,
                'sourcesystem_cd = :ss and not modifier_cd = \'@\'',
                ss: STUDY_ID)

        assertThat numFacts, is(NUMBER_OF_ANNOTATED_FACTS)
    }

    @Test
    void testNumberOfTrialVisitsIsCorrect() {
        long numVisits = rowCounter.count([:], Tables.TRIAL_VISIT_DIMENSION)

        assertThat numVisits, is(NUMBER_OF_VISITS)
    }

    @Test
    void testArbitraryFacts() {
        def facts = queryForList """
            SELECT O.*
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
                INNER JOIN ${Tables.TRIAL_VISIT_DIMENSION} SV
                    ON (O.trial_visit_num = SV.trial_visit_num)
            WHERE patient_num = (
                SELECT patient_num
                FROM ${Tables.PATIENT_DIMENSION}
                WHERE sourcesystem_cd = :subject_id)
            AND C.concept_path = :concept_path
            AND SV.rel_time_label = :visit_label
            """,
                [
                        subject_id  : "TEST_17_1:OBS336-201_05",
                        concept_path: '\\Public Studies\\TEST_17_1\\PKConc\\Timepoint Hrs.\\',
                        visit_label : 'Week 1'
                ]

        assertThat facts, containsInAnyOrder(
                allOf(
                        hasEntry(equalTo('nval_num'), closeTo(0.5, DELTA)),
                        hasEntry(equalTo('start_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry(equalTo('end_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 10).timeInMillis))),
                        hasEntry('instance_num', new BigDecimal('1'))
                ),
                allOf(
                        hasEntry(equalTo('nval_num'), closeTo(1, DELTA)),
                        hasEntry(equalTo('start_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry(equalTo('end_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry('instance_num', new BigDecimal('2'))
                ),
                allOf(
                        hasEntry(equalTo('nval_num'), closeTo(2, DELTA)),
                        hasEntry(equalTo('start_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry(equalTo('end_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 11).timeInMillis))),
                        hasEntry('instance_num', new BigDecimal('3'))
                ),
                allOf(
                        hasEntry(equalTo('nval_num'), closeTo(4, DELTA)),
                        hasEntry(equalTo('start_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry(equalTo('end_date'), hasProperty('time',
                                equalTo(new GregorianCalendar(2016, Calendar.MARCH, 9).timeInMillis))),
                        hasEntry('instance_num', new BigDecimal('4'))
                ),
        )
    }

    @Test
    void testStrictConceptVariables() {
        def facts = queryForList """
            SELECT O.tval_char
            FROM
                ${Tables.OBSERVATION_FACT} O
                INNER JOIN ${Tables.CONCEPT_DIMENSION} C
                    ON (O.concept_cd = C.concept_cd)
                WHERE C.concept_path = :concept_path
            """,
                [
                        concept_path: '\\Public Studies\\TEST_17_1\\Demography\\Sex\\',
                ]

        assertThat facts, everyItem(either(hasEntry('tval_char', 'M')) | hasEntry('tval_char', 'F'))
    }

    /**
     * Tests that after loading the TEST_17_1 study:
     * - there exists generic concepts in the concept dimension, using the SNOMED codes from
     * the mapping file
     * - there exists generic and study specific nodes in the i2b2 tree for those concepts.
     */
    @Test
    void testMappedOntologyCodes() {
        def genericNodePath = '\\Observable entity\\Clinical history/examination observable\\' +
                'General characteristic of patient\\Body measure\\Height / growth measure\\' +
                'Body height measure\\Standing height\\'
        def studySpecificPath = '\\Public Studies\\TEST_17_1\\Vital Signs\\Height CM\\'
        def conceptCode = 'SNOMEDCT/248333004'
        def conceptPath = '\\Ontology\\SNOMEDCT/248333004\\'

        // Test that a generic concept is inserted
        def concept = queryForMap """
            SELECT c.concept_path, c.concept_cd
            FROM ${Tables.CONCEPT_DIMENSION} c
            WHERE c.concept_cd = :concept_cd
            """,
            [ concept_cd: conceptCode ]

        assertThat concept, notNullValue()
        assertThat concept['concept_path'] as String, equalTo(conceptPath)

        // Test that a study specific node and a generic node exist
        def nodes = queryForList """
            SELECT i.c_fullname, i.c_name, i.c_tablename, i.c_columnname, i.c_dimcode
            FROM ${Tables.I2B2_SECURE} i
            WHERE i.c_dimcode = :conceptPath
            """,
            [ conceptPath: conceptPath ]

        assertThat nodes.size(), is(2)
        assertThat nodes, everyItem(
                allOf(
                    hasEntry(is('c_tablename'), equalToIgnoringCase('concept_dimension')),
                    hasEntry(is('c_columnname'), equalToIgnoringCase('concept_path'))
                )
        )
        assertThat nodes, containsInAnyOrder(
                hasEntry(is('c_fullname'), equalTo(studySpecificPath)),
                hasEntry(is('c_fullname'), equalTo(genericNodePath))
        )
    }
}
