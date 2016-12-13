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
                'sourcesystem_cd = :ss',
                ss: STUDY_ID)

        assertThat numFacts, is(NUMBER_OF_FACTS)
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

}
