package org.transmartproject.copy

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.transmartproject.copy.table.*
import spock.lang.Specification

/**
 * Test the data loading functionality of transmart-copy.
 */
class CopySpec extends Specification {

    static final String TEST_STUDY = 'SURVEY0'
    static final String INCREMENTAL_STUDY = 'SURVEY_INC'
    static final String INCREMENTAL_STUDY_SHARED_PATIENT = 'SURVEY_INC2_SHARED_PATIENT'
    static final String STUDY_FOLDER = './src/test/resources/examples/' + TEST_STUDY
    static final String CORRUPTED_STUDY_FOLDER = './src/test/resources/examples/' + TEST_STUDY + '_corrupted'
    static final String INCREMENTAL_STUDY_FOLDER = './src/test/resources/examples/' + INCREMENTAL_STUDY
    static final String INCREMENTAL_STUDY_SHARED_PATIENT_FOLDER =
            './src/test/resources/examples/' + INCREMENTAL_STUDY_SHARED_PATIENT
    static final String CONCEPT_PATH_NAME_UPDATE_SET_FOLDER =
            './src/test/resources/examples/CONCEPT_PATH_NAME_UPDATE'

    static final Map<String, String> DATABASE_CREDENTIALS = [
            PGHOST     : 'localhost',
            PGPORT     : '5432',
            PGDATABASE : 'transmart',
            PGUSER     : 'biomart_user',
            PGPASSWORD : 'biomart_user',
            MAXPOOLSIZE: '5'
    ]

    def studySpecificTables = [
            Observations.TABLE,
            Studies.TRIAL_VISIT_TABLE,
            Studies.STUDY_TABLE,
            Studies.STUDY_DIMENSIONS_TABLE,
    ]

    Copy copy

    static defaultConfig = new Copy.Config(
            write: false
    )

    def setup() {
        copy = new Copy(DATABASE_CREDENTIALS)
    }

    def cleanup() {
        ensureAllStudiesUnloaded()
        copy.close()
    }

    def 'test loading the study data'() {
        given: 'Test database is available, the study is not loaded'
        Map beforeCounts = count(studySpecificTables)
        Map fileRowCounts = count(STUDY_FOLDER, studySpecificTables)
        def expectedConceptPaths = readFieldsFromFile(STUDY_FOLDER, Concepts.TABLE, 'concept_path')
        def expectedModifierPaths = readFieldsFromFile(STUDY_FOLDER, Modifiers.TABLE, 'modifier_path')
        def expectedSubjectIds =
                readFieldsFromFile(STUDY_FOLDER, Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def expectedTreeNodePaths = readFieldsFromFile(STUDY_FOLDER, TreeNodes.TABLE, 'c_fullname')

        when: 'Loading example study data'
        copy.uploadStudy(STUDY_FOLDER, defaultConfig)

        Map afterCounts = count(studySpecificTables)
        Map expectedCounts = fileRowCounts
                .collectEntries { Table table, Number rows -> [table, beforeCounts[table] + rows] }
        def inDbConceptPaths = readFieldsFromDb(Concepts.TABLE, 'concept_path')
        def inDbModifierPaths = readFieldsFromDb(Modifiers.TABLE, 'modifier_path')
        def inDbSubjectIds = readFieldsFromDb(Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def inDbTreeNodePaths = readFieldsFromDb(TreeNodes.TABLE, 'c_fullname')


        then: 'Expect the study to be loaded'
        testStudyDbIdentifier
        expectedCounts == afterCounts
        inDbConceptPaths.containsAll(expectedConceptPaths)
        inDbModifierPaths.containsAll(expectedModifierPaths)
        inDbSubjectIds.containsAll(expectedSubjectIds)
        inDbTreeNodePaths.containsAll(expectedTreeNodePaths)
    }

    def 'test loading the relations data'() {
        given: 'Test database is available, the study is not loaded'
        def expectedRelationTypeLabels = readFieldsFromFile(STUDY_FOLDER, Relations.RELATION_TABLE, 'label')

        when: 'Loading the pedigree data'
        copy.uploadPedigree(STUDY_FOLDER, defaultConfig)

        then: 'Expect the study to be loaded'
        def inDbRelationTypeLabels = readFieldsFromFile(STUDY_FOLDER, Relations.RELATION_TABLE, 'label')
        count([Relations.RELATION_TABLE]) == count(STUDY_FOLDER, [Relations.RELATION_TABLE])
        inDbRelationTypeLabels.containsAll(expectedRelationTypeLabels)
    }

    def 'test deleting the study'() {
        given: 'Test database is available, the study is loaded'
        ensureTestStudyLoaded()
        Map beforeCounts = count(studySpecificTables)
        Map fileRowCounts = count(STUDY_FOLDER, studySpecificTables)

        when: 'Deleting the study'
        copy.deleteStudyById(TEST_STUDY)

        Map afterCounts = count(studySpecificTables)
        Map expectedCounts = fileRowCounts
                .collectEntries { Table table, Number rows -> [table, beforeCounts[table] - rows] }

        then: 'Expect the study has been removed'
        noTestStudyInDb()
        expectedCounts == afterCounts
    }

    def 'test detecting if table exists'() {
        given: 'Test database is available'

        when: 'Checking for a non-existing table'
        def tableBar = new Table('foo', 'bar')
        def tableBarExists = copy.database.tableExists(tableBar)
        then: 'The result is false.'
        !tableBarExists

        when: 'Checking for an existing table'
        def observationsTableExists = copy.database.tableExists(Observations.TABLE)
        then: 'The result is true'
        observationsTableExists
    }

    def 'test index management'() {
        when: 'dropping and restore indexes'
        copy.dropIndexes()
        def afterDropIndexes = copy.database.indexesForTable(Observations.TABLE)
        copy.restoreIndexes()

        then:
        afterDropIndexes == ['observation_fact_pkey'] as Set
        copy.database.indexesForTable(Observations.TABLE) == [
                'observation_fact_pkey',
                'idx_fact_patient_num',
                'idx_fact_trial_visit_num',
                'idx_fact_concept',
                'observation_fact_pct_idx',
                'observation_fact_tmceppis_idx'
        ] as Set
    }

    def 'delete by path when no study'() {
        given: 'there is no study'

        when:
        copy.deleteStudy(STUDY_FOLDER, false)
        then:
        noTestStudyInDb()

        when:
        copy.deleteStudy(STUDY_FOLDER, true)
        then:
        def exception = thrown(IllegalStateException)
        exception.message == 'Study not found: ' + TEST_STUDY + '.'
    }

    def 'delete by path when study loaded'() {
        given: 'there is the study'
        ensureTestStudyLoaded()

        when:
        copy.deleteStudy(STUDY_FOLDER)
        then:
        noTestStudyInDb()
    }

    def 'delete exclude default upload'() {
        given: 'there is the study'
        ensureTestStudyLoaded()
        Options options = new Options()
        options.addOption(new Option('D', 'delete', true, ''))

        when: 'running with delete option'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--delete', TEST_STUDY] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        then: 'study has been deleted'
        noTestStudyInDb()
    }

    def 'test re-upload in single transaction'() {
        given: 'there is a study'
        ensureTestStudyLoaded()
        def studyId1 = testStudyDbIdentifier
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))

        when: 'error during load happens'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--directory', CORRUPTED_STUDY_FOLDER] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        then: 'transaction has rolled back'
        thrown(FileNotFoundException)
        testStudyDbIdentifier == studyId1

        when: 'load is successful'
        CommandLine cli2 = new DefaultParser().parse(options, ['--directory', STUDY_FOLDER] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        then: 'new data have been uploaded'
        testStudyDbIdentifier > studyId1
    }

    def 'test partitioning handling'() {
        given: 'there is no study'
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))
        options.addOption(new Option('p', 'partition', false, ''))
        options.addOption(new Option('D', 'delete', true, ''))
        options.addOption(new Option('u', 'unlogged', false, ''))
        Set<Table> childTablesBefore = copy.database.getChildTables(Observations.TABLE)

        when: 'loading data with partitioning'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--directory', STUDY_FOLDER, '--partition'] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        Set<Table> childTablesAfterLoad = copy.database.getChildTables(Observations.TABLE)
        def studyId1 = testStudyDbIdentifier
        def addedChildTables = (childTablesAfterLoad - childTablesBefore)

        then: 'partitions created'
        addedChildTables.size() == 2
        studyId1
        copy.database.indexesForTable(addedChildTables[0]).size() == 3
        copy.database.indexesForTable(addedChildTables[1]).size() == 3

        when: 'we reupload data with unlogged flag'
        CommandLine cli2 = new DefaultParser()
                .parse(options, ['--directory', STUDY_FOLDER, '--partition', '--unlogged'] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        def studyId2 = testStudyDbIdentifier
        Set<Table> childTablesAfterReupload = copy.database.getChildTables(Observations.TABLE)
        then:
        studyId2 != null
        studyId1 != studyId2
        childTablesAfterReupload.size() == childTablesAfterLoad.size()
        childTablesAfterReupload != childTablesAfterLoad

        when:
        CommandLine cli3 = new DefaultParser()
                .parse(options, ['--delete', TEST_STUDY] as String[])
        Copy.runCopy(cli3, DATABASE_CREDENTIALS)
        then:
        copy.database.getChildTables(Observations.TABLE) == childTablesBefore
        noTestStudyInDb()

        cleanup:
        addedChildTables.each { Table table -> copy.database.dropTable(table, true) }
    }

    def 'test instance num recalculation'() {
        given: 'there is no study'
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))
        options.addOption(new Option('n', 'base-on-max-instance-num', false, ''))
        def maxInstanceNum = readFieldsFromDb(Observations.TABLE, 'instance_num').max() ?: 0

        when: 'study loaded with instance num calculation on'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--directory', STUDY_FOLDER, '--base-on-max-instance-num'] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        then: 'instance_num starts from base'
        def instanceNums = readFieldsFromDb(
                Observations.TABLE,
                'instance_num',
                "where trial_visit_num in (${testTrialVisitsDbIdentifiers.join(',')})")
        instanceNums.min() > maxInstanceNum

        when: 'study loaded with instance num calculation off'
        CommandLine cli2 = new DefaultParser()
                .parse(options, ['--directory', STUDY_FOLDER] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        then: 'instance_num starts from base'
        def zeroBasedInstanceNums = readFieldsFromDb(
                Observations.TABLE,
                'instance_num',
                "where trial_visit_num in (${testTrialVisitsDbIdentifiers.join(',')})")
        zeroBasedInstanceNums.min() == 1
    }

    def 'test incremental loading'() {
        given: 'there is no study'
        Options options = new Options()
        options.addOption(new Option('I', 'incremental', false, ''))
        options.addOption(new Option('d', 'directory', true, ''))


        when: 'loading a first part of a study'
        def studyPart1Folder = INCREMENTAL_STUDY_FOLDER + '/part1'
        Map fileRowCounts1 = count(studyPart1Folder, studySpecificTables)
        def expectedConceptPaths1 = readFieldsFromFile(studyPart1Folder, Concepts.TABLE, 'concept_path')
        def expectedModifierPaths1 = readFieldsFromFile(studyPart1Folder, Modifiers.TABLE, 'modifier_path')
        def expectedSubjectIds1 =
                readFieldsFromFile(studyPart1Folder, Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def expectedTreeNodePaths1 = readFieldsFromFile(studyPart1Folder, TreeNodes.TABLE, 'c_fullname')

        CommandLine cli1 = new DefaultParser().parse(options, ['--directory', studyPart1Folder] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)

        def inDbPatientNums1 = readFieldsFromDb(Observations.TABLE,'patient_num') as Set
        Map countsAfterUpload1 = count(studySpecificTables)
        def inDbConceptPaths1 = readFieldsFromDb(Concepts.TABLE, 'concept_path')
        def inDbModifierPaths1 = readFieldsFromDb(Modifiers.TABLE, 'modifier_path')
        def inDbSubjectIds1 = readFieldsFromDb(Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def inDbTreeNodePaths1 = readFieldsFromDb(TreeNodes.TABLE, 'c_fullname')


        then: 'the first part of the study has been loaded'
        incrementalStudyDbIdentifier
        fileRowCounts1 == countsAfterUpload1
        inDbPatientNums1.size() == expectedSubjectIds1.size()
        inDbConceptPaths1.containsAll(expectedConceptPaths1)
        inDbModifierPaths1.containsAll(expectedModifierPaths1)
        inDbSubjectIds1.containsAll(expectedSubjectIds1)
        inDbTreeNodePaths1.containsAll(expectedTreeNodePaths1)


        when: 'loading a second part of the study with incremental option'
        def studyPart2Folder = INCREMENTAL_STUDY_FOLDER + '/part2'
        def expectedConceptPaths2 = readFieldsFromFile(studyPart2Folder, Concepts.TABLE, 'concept_path')
        def expectedModifierPaths2 = readFieldsFromFile(studyPart2Folder, Modifiers.TABLE, 'modifier_path')
        def expectedSubjectIds2 =
                readFieldsFromFile(studyPart2Folder, Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def expectedTreeNodePaths2 = readFieldsFromFile(studyPart2Folder, TreeNodes.TABLE, 'c_fullname')

        CommandLine cli2 = new DefaultParser()
                .parse(options, ['--incremental', '--directory', studyPart2Folder] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)

        Map countsAfterUpload2 = count(studySpecificTables)
        def inDbPatientNums2 = readFieldsFromDb(Observations.TABLE,'patient_num') as Set
        def inDbConceptPaths2 = readFieldsFromDb(Concepts.TABLE, 'concept_path')
        def inDbModifierPaths2 = readFieldsFromDb(Modifiers.TABLE, 'modifier_path')
        def inDbSubjectIds2 = readFieldsFromDb(Patients.PATIENT_MAPPING_TABLE, 'patient_ide')
        def inDbPatientIdeToPatientNum = readPatientIdeToPatientNum()
        def inDbTreeNodePaths2 = readFieldsFromDb(TreeNodes.TABLE, 'c_fullname')


        then: 'the second part of the study has been loaded incrementally'
        incrementalStudyDbIdentifier
        /** 1 patient not present in part2: SURVEY_INC_P2 -> 3 observations
         *  1 patient updated in part2    : SURVEY_INC_P1 -> 3 observations
         *  1 patient added in part 2     : SURVEY_INC_P3 -> 3 observations
         */
        inDbPatientNums2.size() == inDbPatientNums1.size() + 1
        countsAfterUpload2[Observations.TABLE] == countsAfterUpload1[Observations.TABLE] + 3
        countsAfterUpload2[Studies.TRIAL_VISIT_TABLE] == countsAfterUpload1[Studies.TRIAL_VISIT_TABLE] + 1
        countsAfterUpload2[Studies.STUDY_TABLE] == countsAfterUpload1[Studies.STUDY_TABLE]
        countsAfterUpload2[Studies.STUDY_DIMENSIONS_TABLE] == countsAfterUpload1[Studies.STUDY_DIMENSIONS_TABLE]
        inDbConceptPaths2.containsAll(expectedConceptPaths2)
        inDbModifierPaths2.containsAll(expectedModifierPaths2)
        inDbSubjectIds2.containsAll(expectedSubjectIds2)
        inDbTreeNodePaths2.containsAll(expectedTreeNodePaths2)
        inDbSubjectIds2.findAll{ it.toString().startsWith("SURVEY_INC_")} as Set ==
                (expectedSubjectIds1 + expectedSubjectIds2) as Set

        // observation for patient from part1 only - old value
        readFieldsFromDb(Observations.TABLE,'nval_num',
                "where patient_num=${inDbPatientIdeToPatientNum['SURVEY_INC_P2']} AND concept_cd='age'") == [33]
        // observation for patient updated in part2 - updated value
        readFieldsFromDb(Observations.TABLE,'nval_num',
                "where patient_num=${inDbPatientIdeToPatientNum['SURVEY_INC_P1']} AND concept_cd='age'") == [26]
        // observation for patient added in part2 - new value
        readFieldsFromDb(Observations.TABLE,'nval_num',
                "where patient_num=${inDbPatientIdeToPatientNum['SURVEY_INC_P3']} AND concept_cd='age'") == [60]
    }

    def 'test incremental loading of the same data'() {
        given: 'there is a study'
        ensureTestStudyLoaded()
        Map beforeCounts = count(studySpecificTables)
        Options options = new Options()
        options.addOption(new Option('I', 'incremental', false, ''))
        options.addOption(new Option('d', 'directory', true, ''))

        when: 'loading the same study with incremental option'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--incremental', '--directory', STUDY_FOLDER] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        Map afterCounts = count(studySpecificTables)

        then: 'study has not been deleted'
        testStudyDbIdentifier
        beforeCounts[Observations.TABLE] == afterCounts[Observations.TABLE]
        beforeCounts[Studies.STUDY_TABLE] == afterCounts[Studies.STUDY_TABLE]
        beforeCounts[Studies.STUDY_DIMENSIONS_TABLE] == afterCounts[Studies.STUDY_DIMENSIONS_TABLE]
        // additional trial visits linked to the 2 new observations
        beforeCounts[Studies.TRIAL_VISIT_TABLE] + 2 == afterCounts[Studies.TRIAL_VISIT_TABLE]
    }

    def 'test incremental loading with patient present in multiple studies'() {
        given: 'there are 2 studies with the same patient'
        copy.uploadStudy(INCREMENTAL_STUDY_SHARED_PATIENT_FOLDER, defaultConfig)
        copy.uploadStudy(INCREMENTAL_STUDY_FOLDER + '/part1', defaultConfig)
        Options options = new Options()
        options.addOption(new Option('I', 'incremental', false, ''))
        options.addOption(new Option('d', 'directory', true, ''))
        def inDbPatientIdeToPatientNum = readPatientIdeToPatientNum()
        def observationsForSharedUserBeforeUpdate = readFieldsFromDb(Observations.TABLE,'nval_num',
                "where patient_num=${inDbPatientIdeToPatientNum['SURVEY_INC_P1']} AND concept_cd='age'")

        when: 'loading one of the studies incrementally'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--incremental', '--directory', INCREMENTAL_STUDY_FOLDER + '/part2'] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        def observationsForSharedUserAfterUpdate = readFieldsFromDb(Observations.TABLE,'nval_num',
                "where patient_num=${inDbPatientIdeToPatientNum['SURVEY_INC_P1']} AND concept_cd='age'")

        then: 'observations from a different study for the shared patient are not changed'
        observationsForSharedUserBeforeUpdate.size() == 2
        observationsForSharedUserBeforeUpdate.containsAll([42.0000000000000000, 24.0000000000000000])
        observationsForSharedUserAfterUpdate.size() == 2
        observationsForSharedUserAfterUpdate.containsAll([42.0000000000000000, 26.0000000000000000])
    }

    def 'test concept and tree nodes updating'() {
        given: 'there are concepts and tree nodes in the database'
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))
        CommandLine cli1 = new DefaultParser().parse(options,
                ['--directory', "$CONCEPT_PATH_NAME_UPDATE_SET_FOLDER/part1"] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)

        when: 'trying to upload concepts with duplicated codes'
        CommandLine cli2 = new DefaultParser().parse(options,
                ['--directory', "$CONCEPT_PATH_NAME_UPDATE_SET_FOLDER/part2"] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        then: 'exception is thrown'
        def exception = thrown(IllegalStateException)
        exception.message.startsWith('Cannot load concept with code')
        exception.message.endsWith('Other concept already exists with that path.')

        when: 'trying to upload concepts with duplicated codes and update-concept-paths flag'
        options.addOption(new Option('U', 'update-concept-paths', false, ''))
        CommandLine cli3 = new DefaultParser().parse(options,
                ['--directory', "$CONCEPT_PATH_NAME_UPDATE_SET_FOLDER/part2", '--update-concept-paths'] as String[])
        Copy.runCopy(cli3, DATABASE_CREDENTIALS)
        def expectedConceptNames = ['02. Diagnosis Data', '02. Patient Data', '01. Birth date',
                                    'Biological taxonomy', '02. Type of tumor']
        def expectedConceptPaths = ['\\Projects\\CPNU\\01. Patient information\\02. Patient Data\\',
                                    '\\Projects\\CPNU\\02. Diagnosis information\\02. Diagnosis Data\\',
                                    '\\Projects\\CPNU\\01. Patient information\\01. Birth date\\',
                                    '\\Projects\\CPNU\\01. Patient information\\Taxonomy\\',
                                    '\\Projects\\CPNU\\02. Diagnosis information\\02. Type of tumor\\']
        def inDbConceptPaths = readFieldsFromDb(Concepts.TABLE,
                'concept_path',
                "where concept_path like '\\\\Projects\\\\CPNU\\\\%'")
        def inDbConceptNames = readFieldsFromDb(Concepts.TABLE,
                'name_char',
                "where concept_path like '\\\\Projects\\\\CPNU\\\\%'")
        def expectedNodePathForBirthDateConcept = readFieldsFromDb(TreeNodes.TABLE,
                'c_fullname',
                "where c_basecode = 'Patient.birth_date'")
        def expectedConceptPathForBirthDateConcept = readFieldsFromDb(TreeNodes.TABLE,
                'c_dimcode',
                "where c_basecode = 'Patient.birth_date'")

        then: 'concept path, names and tree nodes are updated'
        inDbConceptPaths.size() == 5
        inDbConceptPaths.containsAll(expectedConceptPaths)
        inDbConceptNames.containsAll(expectedConceptNames)
        expectedConceptPathForBirthDateConcept.size() == 1
        expectedConceptPathForBirthDateConcept[0] == '\\Projects\\CPNU\\01. Patient information\\01. Birth date\\'
        expectedNodePathForBirthDateConcept.size() == 1
        expectedNodePathForBirthDateConcept[0] == '\\Projects\\CPNU\\01. Patient information\\01. Date of birth\\'
    }

    List<Number> getTestTrialVisitsDbIdentifiers() {
        readFieldsFromDb(
                Studies.TRIAL_VISIT_TABLE,
                'trial_visit_num',
                "where study_num='${testStudyDbIdentifier}'")
    }

    Number getTestStudyDbIdentifier() {
        def list = readFieldsFromDb(Studies.STUDY_TABLE, 'study_num', "where study_id='${TEST_STUDY}'")
        list ? list.first() : null
    }

    Number getIncrementalStudyDbIdentifier() {
        def list = readFieldsFromDb(Studies.STUDY_TABLE, 'study_num', "where study_id='${INCREMENTAL_STUDY}'")
        list ? list.first() : null
    }

    Number getIncrementalStudySharedPatientDbIdentifier() {
        def list = readFieldsFromDb(Studies.STUDY_TABLE, 'study_num',
                "where study_id='${INCREMENTAL_STUDY_SHARED_PATIENT}'")
        list ? list.first() : null
    }

    void ensureTestStudyLoaded() {
        if (noTestStudyInDb()) {
            copy.uploadStudy(STUDY_FOLDER, defaultConfig)
            assert testStudyDbIdentifier
        }
    }

    void ensureAllStudiesUnloaded() {
        copy.deleteStudyById(TEST_STUDY, false)
        copy.deleteStudyById(INCREMENTAL_STUDY, false)
        copy.deleteStudyById(INCREMENTAL_STUDY_SHARED_PATIENT, false)
        assert noTestStudyInDb()
        assert noIncrementalStudyInDb()
        assert noIncrementalStudySharedPatientInDb()
    }

    boolean noTestStudyInDb() {
        testStudyDbIdentifier == null
    }

    boolean noIncrementalStudyInDb() {
        incrementalStudyDbIdentifier == null
    }

    boolean noIncrementalStudySharedPatientInDb() {
        incrementalStudySharedPatientDbIdentifier == null
    }

    Map<Table, Number> count(Iterable<Table> tables) {
        tables.collectEntries { Table table ->
            [
                    table,
                    copy.database.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ${table}", new Object[0], Long)
            ]
        }
    }

    Map<Table, Number> count(String folder, Iterable<Table> tables) {
        tables.collectEntries { Table table ->
            def rows = 0
            new File(folder, table.fileName).withReader { reader ->
                rows = Util.tsvReader(reader).iterator().count { it } - 1
            }
            [
                    table,
                    rows
            ]
        }
    }

    Map readPatientIdeToPatientNum() {
        readFieldsFromDb(Patients.PATIENT_MAPPING_TABLE, ['patient_ide', 'patient_num']).collectEntries {
            [(it['patient_ide']): it['patient_num']]
        }
    }

    List readFieldsFromDb(Table table, String field, String where = '') {
        readFieldsFromDb(table, [field], where).collect { it[field] }
    }

    List<Map> readFieldsFromDb(Table table, List<String> selectFields, String where = '') {
        copy.database.jdbcTemplate.queryForList(
                "SELECT ${selectFields.join(', ')} FROM ${table}" + (where ? ' ' + where : ''))
    }

    List readFieldsFromFile(String folder, Table table, String selectField) {
        readFieldsFromFile(folder, table, [selectField]).collect { it[selectField] }
    }

    List<Map> readFieldsFromFile(String folder, Table table, List<String> selectFields) {
        List<Map> result = []
        new File(folder, table.fileName).withReader { reader ->
            def headerIndex = [:]
            Util.tsvReader(reader).eachWithIndex { String[] row, int index ->
                if (index == 0) {
                    headerIndex = selectFields.collectEntries { String selectField ->
                        [selectField, row.findIndexOf {
                            it == selectField
                        }]
                    }
                } else {
                    result << headerIndex.collectEntries { header, column -> [header, row[column]] }
                }
            }
        }
        result
    }
}
