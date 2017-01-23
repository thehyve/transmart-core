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
class ClinicalDataCleanScenarioConceptTypeTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581_CONCEPT_TYPES'

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(STUDY_ID, 'clinical')

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    //test that numericConcepts are uploaded as Categorical is specified so in the column mapping file
    void testCorrectlySpecifiedConpceptTypes() {

        def q = """
            SELECT DISTINCT C.name_char, O.valtype_cd
            FROM ${Tables.OBSERVATION_FACT} O
            INNER JOIN ${Tables.CONCEPT_DIMENSION} C on C.concept_cd = O.concept_cd
            WHERE O.sourcesystem_cd = :ss"""

        def r = queryForList q, [ss: STUDY_ID]

        assertThat r, containsInAnyOrder(
                allOf(hasEntry('name_char', 'FEV1'),  hasEntry('valtype_cd', 'N')),
                allOf(hasEntry('name_char', 'AfricanAmerican'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', 'Caucasian'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '0'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '1'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '2'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '3'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '4'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '5'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '6'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '7'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', '8'),  hasEntry('valtype_cd', 'T')),
                allOf(hasEntry('name_char', 'test'),  hasEntry('valtype_cd', 'T')),
        )
    }
}
