package org.transmartproject.batch.i2b2

import org.hamcrest.Matcher
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.junit.JobRunningTestTrait
import org.transmartproject.batch.junit.LoadTablesRule
import org.transmartproject.batch.junit.RunJobRule
import org.transmartproject.batch.support.TableLists

import java.text.SimpleDateFormat

import static java.util.concurrent.TimeUnit.MINUTES
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static uk.co.it.modular.hamcrest.date.DateMatchers.within

/**
 * Test an i2b2 non-incremental job on a clean database.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class I2b2NonIncrementalTests implements JobRunningTestTrait {

    public final static String DATA_DIR = 'I2B2SAMPLE'

    public final static String SOURCE_SYSTEM = 'TEST_DATA'

    public final static long NUMBER_OF_PATIENTS = 3L
    public final static long NUMBER_OF_VISITS = 3L
    public final static long NUMBER_OF_PROVIDERS = 2L
    public final static long NUMBER_OF_FACTS = 11L
    public final static long NUMBER_OF_MODIFIER_FACTS = 3L

    private final SimpleDateFormat sdf =
            new SimpleDateFormat('yyyy-MM-dd HH:mm:ss', Locale.ENGLISH).with {
                timeZone = TimeZone.getTimeZone('Europe/Amsterdam')
                it
            }

    private final Date downloadDate = sdf.parse('2014-03-30 18:00:00')

    private static final String PROJECT_ID = 'test_project'
    private static final String PATIENT_IDE_SOURCE = 'Test data patient source'

    @ClassRule
    public final static TestRule RUN_JOB_RULE = new RuleChain([
            new RunJobRule(DATA_DIR, 'i2b2'),
            new LoadTablesRule(
                    (Tables.I2B2): new ClassPathResource('i2b2/i2b2.tsv'),
                    (Tables.CONCEPT_DIMENSION): new ClassPathResource('i2b2/concept_dimension.tsv')),
    ])
    public static final String ENCOUNTER_IDE_SOURCE = 'Test data visit source'

    @AfterClass
    static void cleanDatabase() {
        PersistentContext.truncator.
                truncate(TableLists.I2B2_TABLES + 'ts_batch.batch_job_instance')
    }

    @Test
    void testNumberOfPatients() {
        assertThat rowCounter.count(Tables.PATIENT_DIMENSION),
                is(NUMBER_OF_PATIENTS)

        assertThat rowCounter.count(Tables.PATIENT_MAPPING),
                is(NUMBER_OF_PATIENTS)
    }

    @Test
    void testPatientValues() {
        Map<String, Object> result = queryForMap(
                "SELECT patient_ide_source, sex_cd, religion_cd, " +
                        "statecityzip_path, patient_blob, project_id, " +
                        "${adminColumns.collect { "M.$it AS $it" }.join ', '} " +
                        "FROM ${Tables.PATIENT_MAPPING} M " +
                        "INNER JOIN ${Tables.PATIENT_DIMENSION} D ON (D.patient_num = M.patient_num) " +
                        "WHERE patient_ide = :patientIde",
                [patientIde: 'patient 1'])

        assertThat result, allOf(
                hasEntry('project_id', PROJECT_ID),
                hasEntry('patient_ide_source', PATIENT_IDE_SOURCE),
                hasEntry('sex_cd', 'DEM|SEX:m'),
                hasEntry('religion_cd', 'DEM|RELIGION:agnostic'),
                hasEntry('statecityzip_path', 'statezipcity_path of patient 1'),
                hasEntry('patient_blob', 'Patient 1 blob'),
                adminColumnsMatcher
        )
    }

    @Test
    void testNumberOfVisits() {
        assertThat rowCounter.count(Tables.VISIT_DIMENSION),
                is(NUMBER_OF_VISITS)

        assertThat rowCounter.count(Tables.ENCOUNTER_MAPPING),
                is(NUMBER_OF_VISITS)
    }

    @Test
    void testVisitValues() {
        Map<String, Object> result = queryForMap(
                "SELECT encounter_ide_source, project_id, patient_ide, patient_ide_source, " +
                        "active_status_cd, start_date, end_date, length_of_stay, visit_blob, " +
                        "${adminColumns.collect { "M.$it AS $it" }.join ', '} " +
                        "FROM ${Tables.ENCOUNTER_MAPPING} M " +
                        "INNER JOIN ${Tables.VISIT_DIMENSION} D ON (D.encounter_num = M.encounter_num) " +
                        "WHERE encounter_ide = :encounterIde",
                [encounterIde: 'visit 1'])

        assertThat result, allOf(
                hasEntry('encounter_ide_source', ENCOUNTER_IDE_SOURCE),
                hasEntry('project_id', PROJECT_ID),
                hasEntry('patient_ide', 'patient 1'),
                hasEntry('patient_ide_source', PATIENT_IDE_SOURCE),
                hasEntry('active_status_cd', 'F'),
                hasEntry(is('start_date'), within(1, SECONDS, sdf.parse('2015-03-30 16:00:00'))),
                hasEntry(is('end_date'), within(1, SECONDS, sdf.parse('2015-03-30 17:30:00'))),
                hasEntry('length_of_stay', BigDecimal.valueOf(0L)),
                hasEntry('visit_blob', 'visit 1 blob'),
                adminColumnsMatcher)
    }

    @Test
    void testAutoGeneratedVisit() {
        long count = rowCounter.count(Tables.ENCOUNTER_MAPPING,
                'encounter_ide = :encounterIde',
                encounterIde: 'patient 3@2014-03-17')
        assertThat count, is(1L)
    }

    @Test
    void testNumberOfProviders() {
        assertThat rowCounter.count(Tables.PROV_DIMENSION),
                is(NUMBER_OF_PROVIDERS)
    }

    @Test
    void testProviderValues() {
        Map<String, Object> result = queryForMap(
                "SELECT provider_path, name_char, provider_blob, " +
                        "${adminColumns.join ', '} " +
                        "FROM ${Tables.PROV_DIMENSION} M " +
                        "WHERE provider_id = :providerId",
                [providerId: 'provider 1'])

        assertThat result, allOf(
                hasEntry('provider_path', '/test data'),
                hasEntry('name_char', 'Name of Provider 1'),
                hasEntry('provider_blob', 'Blob of Provider 1'),
                adminColumnsMatcher)
    }

    @Test
    void testAutoGeneratedProvider() {
        assertThat rowCounter.count(Tables.PROV_DIMENSION,
                'provider_id = :providerId',
                providerId: 'Provider for TEST_DATA'), is(1L)
    }

    @Test
    void testNumberOfObservationFactEntries() {
        assertThat rowCounter.count(Tables.OBSERVATION_FACT, 'modifier_cd = \'@\''),
                is(NUMBER_OF_FACTS)

        assertThat rowCounter.count(Tables.OBSERVATION_FACT, 'modifier_cd <> \'@\''),
                is(NUMBER_OF_MODIFIER_FACTS)
    }

    @Test
    void testFactCommonColumns() {
        Map<String, Object> result = queryForMap(
                """SELECT provider_id, start_date, end_date,
                        ${(adminColumns.collect { "F.$it" }.join(', '))}
                        FROM $joinedFactTables
                        WHERE $patient1FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd
                        AND instance_num = :instanceNum""",
                [conceptCd: 'C1-TEXT', instanceNum: 1, modifierCd: '@'])

        assertThat result, allOf(
                hasEntry('provider_id', 'provider 1'),
                hasEntry(is('start_date'), within(1, SECONDS, sdf.parse('2015-03-30 16:21:23.922'))),
                hasEntry(is('end_date'), within(1, SECONDS, sdf.parse('2015-03-30 17:00:00'))),
                adminColumnsMatcher)
    }

    @Test
    void testTextFact() {
        Map<String, Object> result = queryForMap(
                """SELECT F.valtype_cd, F.tval_char, F.nval_num, F.observation_blob
                        FROM $joinedFactTables
                        WHERE $patient1FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd
                        AND instance_num = :instanceNum""",
                [conceptCd: 'C1-TEXT', instanceNum: 1, modifierCd: '@'])

        assertThat result, allOf(
                hasEntry('valtype_cd', 'T'),
                hasEntry('tval_char', 'text of patient 1'),
                hasEntry(is('nval_num'), nullValue()),
                hasEntry(is('observation_blob'), nullValue()))
    }

    @Test
    void testNumberFact() {
        Map<String, Object> result = queryForMap(
                """SELECT F.valtype_cd, F.tval_char, F.nval_num, F.observation_blob
                        FROM $joinedFactTables
                        WHERE $patient1FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd
                        AND instance_num = :instanceNum""",
                [conceptCd: 'C3-NUMBER', instanceNum: 1, modifierCd: '@'])

        assertThat result, allOf(
                hasEntry('valtype_cd', 'N'),
                hasEntry('tval_char', 'G'),
                hasEntry('nval_num', 5.00001),
                hasEntry(is('observation_blob'), nullValue()))
    }

    @Test
    void testBlobFact() {
        Map<String, Object> result = queryForMap(
                """SELECT F.valtype_cd, F.tval_char, F.nval_num, F.observation_blob
                        FROM $joinedFactTables
                        WHERE $patient1FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd
                        AND instance_num = :instanceNum""",
                [conceptCd: 'C2-BLOB', instanceNum: 1, modifierCd: '@'])

        assertThat result, allOf(
                hasEntry('valtype_cd', 'B'),
                hasEntry(is('tval_char'), nullValue()),
                hasEntry(is('nval_num'), nullValue()),
                hasEntry(is('observation_blob'), equalTo('my blob')))
    }

    @Test
    void testNLPFact() {
        Map<String, Object> result = queryForMap(
                """SELECT F.valtype_cd, F.tval_char, F.nval_num, F.observation_blob
                        FROM $joinedFactTables
                        WHERE $patient2FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd
                        AND instance_num = :instanceNum""",
                [conceptCd: 'C4-NLP', instanceNum: 1, modifierCd: '@'])

        assertThat result, allOf(
                hasEntry('valtype_cd', 'B'),
                hasEntry(is('tval_char'), nullValue()),
                hasEntry(is('nval_num'), nullValue()),
                hasEntry(is('observation_blob'), equalTo('<a/>')))
    }

    @Test
    void testPatient1HasModifierOnSecondFactGroup() {
        Map<String, Object> result = queryForMap(
                """SELECT instance_num, tval_char
                        FROM $joinedFactTables
                        WHERE $patient1FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd""",
                [conceptCd: 'C1-TEXT', modifierCd: '1'])

        assertThat result, allOf(
                hasEntry('instance_num', BigDecimal.valueOf(2L)),
                hasEntry('tval_char', 'diag 1-2'))
    }

    @Test
    void testPatient2HasModifiersOnTwoGroups() {
        List<Map<String, Object>> result = queryForList(
                """SELECT instance_num, tval_char
                        FROM $joinedFactTables
                        WHERE $patient2FactConditions
                        AND concept_cd = :conceptCd AND modifier_cd = :modifierCd""",
                [conceptCd: 'C1-TEXT', modifierCd: '1'])

        assertThat result, containsInAnyOrder(
                allOf(
                        hasEntry('instance_num', BigDecimal.valueOf(1L)),
                        hasEntry('tval_char', 'diag 2-1')),
                allOf(
                        hasEntry('instance_num', BigDecimal.valueOf(2L)),
                        hasEntry('tval_char', 'diag 2-2')))
    }

    private String getJoinedFactTables() {
        "${Tables.OBSERVATION_FACT} F\n" +
                "INNER JOIN ${Tables.PATIENT_MAPPING} M ON (F.patient_num = M.patient_num)\n" +
                "INNER JOIN ${Tables.ENCOUNTER_MAPPING} N ON (F.encounter_num = N.encounter_num)\n"
    }

    private String getPatient1FactConditions() {
        """N.encounter_ide = 'visit 1'
            AND M.patient_ide = 'patient 1'"""
    }

    private String getPatient2FactConditions() {
        """N.encounter_ide = 'visit 2'
            AND M.patient_ide = 'patient 2'"""
    }

    private List<String> getAdminColumns() {
        ['download_date', 'import_date', 'sourcesystem_cd', 'upload_id']
    }

    private Matcher getAdminColumnsMatcher() {
        allOf(
                hasEntry(is('download_date'), within(1, SECONDS, downloadDate)),
                hasEntry(is('import_date'), within(5, MINUTES, new Date())),
                hasEntry('sourcesystem_cd', SOURCE_SYSTEM),
                hasEntry(is('upload_id'), isA(Number)),
        )
    }

}
