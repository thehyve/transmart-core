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
 * Insertion of a private clinical study.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class ClinicalDataPrivateStudyTests implements JobRunningTestTrait {

    public static final String STUDY_ID = 'GSE8581'

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RunJobRule(
            "${STUDY_ID}_simple",
            'clinical',
            ['-d', 'SECURITY_REQUIRED=Y'])

    @AfterClass
    static void cleanDatabase() {
        // TODO: implement backout study and call it here
        PersistentContext.truncator.
                truncate(TableLists.CLINICAL_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testTopNodeIsUnderPrivateStudies() {
        def q = """
            SELECT c_fullname
            FROM ${Tables.I2B2}
            WHERE sourcesystem_cd = :study
        """
        def res = queryForList(q, [study: STUDY_ID], String)

        assertThat res, everyItem(
                startsWith('\\Private Studies\\GSE8581\\'))
    }

    @Test
    void testSecureObjectTokenIsCorrect() {
        def qSec = """
            SELECT DISTINCT secure_obj_token
            FROM ${Tables.I2B2_SECURE} I
            WHERE sourcesystem_cd = :study"""
        String sot = jdbcTemplate.queryForObject(qSec, [study: STUDY_ID], String)

        assertThat sot, is("EXP:$STUDY_ID" as String)
    }

    @Test
    void testBioExperiment() {
        def q = """
                SELECT bio_experiment_id, etl_id, title
                FROM ${Tables.BIO_EXPERIMENT}
                WHERE accession = :study"""

        def res = queryForList(q, [study: STUDY_ID])

        assertThat res, hasSize(1)
        assertThat res[0], allOf(
                hasEntry('title', 'Metadata not available'),
                hasEntry('etl_id', "METADATA:$STUDY_ID" as String),
                hasEntry(is('bio_experiment_id'), isA(Number)))
    }

    @Test
    void testSecureObject() {
        def q = """
                SELECT S.display_name,
                        S.data_type,
                        S.search_secure_object_id,
                        B.accession
                FROM ${Tables.SECURE_OBJECT} S
                LEFT JOIN ${Tables.BIO_EXPERIMENT} B ON (S.bio_data_id = B.bio_experiment_id)
                WHERE S.bio_data_unique_id = :sot"""
        def res = queryForList(q, [sot: "EXP:$STUDY_ID" as String])

        assertThat res, hasSize(1)
        assertThat res[0], allOf(
                hasEntry('display_name', "Private Studies - $STUDY_ID" as String),
                hasEntry('data_type', 'BIO_CLINICAL_TRIAL'),
                hasEntry(is('search_secure_object_id'), isA(Number)),
                hasEntry('accession', STUDY_ID),)
    }


}
