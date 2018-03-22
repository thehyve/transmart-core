package org.transmartproject.copy

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.transmartproject.copy.table.*
import spock.lang.Specification

class CopySpec extends Specification {

    static String TEST_STUDY = 'SURVEY0'
    static String STUDY_FOLDER = './src/main/resources/examples/' + TEST_STUDY
    static String CORRUPTED_STUDY_FOLDER = './src/main/resources/examples/' + TEST_STUDY + '_corrupted'
    static Map<String, String> DATABASE_CREDENTIALS = [
            PGHOST    : 'localhost',
            PGPORT    : 5432,
            PGDATABASE: 'transmart',
            PGUSER    : 'i2b2demodata',
            PGPASSWORD: 'i2b2demodata'
    ]

    def studySpecificTables = [
            Observations.table,
            Studies.trial_visit_table,
            Studies.study_table,
            Studies.study_dimensions_table,
    ]

    Copy copy

    static defaultConfig = new Copy.Config(
            write: false
    )

    def setup() {
        copy = new Copy(DATABASE_CREDENTIALS)
    }

    def cleanup() {
        copy.close()
    }

    def 'test loading the study data'() {
        given: 'Test database is available, the study is not loaded'
        ensureTestStudyUnloaded()
        Map beforeCounts = count(studySpecificTables)
        Map fileRowCounts = count(STUDY_FOLDER, studySpecificTables)
        def expectedConceptPaths = readFieldsFromFile(STUDY_FOLDER, Concepts.table, 'concept_path')
        def expectedModifierPaths = readFieldsFromFile(STUDY_FOLDER, Modifiers.table, 'modifier_path')
        def expectedSubjectIds = readFieldsFromFile(STUDY_FOLDER, Patients.patient_mapping_table, 'patient_ide')
        def expectedTreeNodePaths = readFieldsFromFile(STUDY_FOLDER, TreeNodes.table, 'c_fullname')

        when: 'Loading example study data'
        copy.uploadStudy(STUDY_FOLDER, defaultConfig)

        Map afterCounts = count(studySpecificTables)
        Map expectedCounts = fileRowCounts
                .collectEntries { Table table, Number rows -> [table, beforeCounts[table] + rows] }
        def inDbConceptPaths = readFieldsFromDb(Concepts.table, 'concept_path')
        def inDbModifierPaths = readFieldsFromDb(Modifiers.table, 'modifier_path')
        def inDbSubjectIds = readFieldsFromDb(Patients.patient_mapping_table, 'patient_ide')
        def inDbTreeNodePaths = readFieldsFromDb(TreeNodes.table, 'c_fullname')


        then: 'Expect the study to be loaded'
        getTestStudyDbIdentifier()
        expectedCounts == afterCounts
        inDbConceptPaths.containsAll(expectedConceptPaths)
        inDbModifierPaths.containsAll(expectedModifierPaths)
        inDbSubjectIds.containsAll(expectedSubjectIds)
        inDbTreeNodePaths.containsAll(expectedTreeNodePaths)
    }

    def 'test loading the relations data'() {
        given: 'Test database is available, the study is not loaded'

        def expectedRelationTypeLabels = readFieldsFromFile(STUDY_FOLDER, Relations.relation_table, 'label')

        when: 'Loading the pedigree data'
        copy.uploadPedigree(STUDY_FOLDER, defaultConfig)

        then: 'Expect the study to be loaded'
        def inDbRelationTypeLabels = readFieldsFromFile(STUDY_FOLDER, Relations.relation_table, 'label')
        count([Relations.relation_table]) == count(STUDY_FOLDER, [Relations.relation_table])
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
        def observationsTableExists = copy.database.tableExists(Observations.table)
        then: 'The result is true'
        observationsTableExists
    }

    def 'test index management'() {
        when: 'dropping and restore indexes'
        copy.dropIndexes()
        def afterDropIndexes = copy.database.indexesForTable(Observations.table)
        copy.restoreIndexes()

        then:
        afterDropIndexes == ['observation_fact_pkey'] as Set
        copy.database.indexesForTable(Observations.table) == [
                'observation_fact_pkey',
                'idx_fact_patient_num',
                'idx_fact_trial_visit_num',
                'idx_fact_concept',
                'observation_fact_pct_idx'
        ] as Set
    }

    def 'delete by path when no study'() {
        given: 'there is no study'
        ensureTestStudyUnloaded()

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

        when: 'error during load happens'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--delete', TEST_STUDY] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        then: 'study has been deleted'
        noTestStudyInDb()
    }

    def 'test re-upload in single transaction'() {
        given: 'there is a study'
        ensureTestStudyLoaded()
        def studyId1 = getTestStudyDbIdentifier()
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))

        when: 'error during load happens'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--directory', CORRUPTED_STUDY_FOLDER] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        then: 'transaction has rolled back'
        thrown(FileNotFoundException)
        getTestStudyDbIdentifier() == studyId1

        when: 'load is successful'
        CommandLine cli2 = new DefaultParser().parse(options, ['--directory', STUDY_FOLDER] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        then: 'new data have been uploaded'
        getTestStudyDbIdentifier() > studyId1
    }


    def 'test partitioning handling'() {
        given: 'there is no study'
        ensureTestStudyUnloaded()
        Options options = new Options()
        options.addOption(new Option('d', 'directory', true, ''))
        options.addOption(new Option('p', 'partition', false, ''))
        options.addOption(new Option('D', 'delete', true, ''))
        Set<Table> childTablesBefore = copy.database.getChildTables(Observations.table)

        when: 'loading data with partitioning'
        CommandLine cli1 = new DefaultParser()
                .parse(options, ['--directory', STUDY_FOLDER, '--partition'] as String[])
        Copy.runCopy(cli1, DATABASE_CREDENTIALS)
        Set<Table> childTablesAfterLoad = copy.database.getChildTables(Observations.table)
        then: 'partitions created'
        def addedChildTables = (childTablesAfterLoad - childTablesBefore)
        addedChildTables.size() == 2
        getTestStudyDbIdentifier()
        copy.database.indexesForTable(addedChildTables[0]).size() == 4
        copy.database.indexesForTable(addedChildTables[1]).size() == 4

        when:
        CommandLine cli2 = new DefaultParser()
                .parse(options, ['--delete', TEST_STUDY] as String[])
        Copy.runCopy(cli2, DATABASE_CREDENTIALS)
        then:
        copy.database.getChildTables(Observations.table) == childTablesBefore
        noTestStudyInDb()

        cleanup:
        addedChildTables.each { Table table -> copy.database.dropTable(table, true) }
    }

    Number getTestStudyDbIdentifier() {
        def list = readFieldsFromDb(Studies.study_table, 'study_num', "where study_id='${TEST_STUDY}'")
        list ? list.first() : null
    }

    void ensureTestStudyLoaded() {
        if (noTestStudyInDb()) {
            copy.uploadStudy(STUDY_FOLDER, defaultConfig)
            assert getTestStudyDbIdentifier()
        }
    }

    void ensureTestStudyUnloaded() {
        if (getTestStudyDbIdentifier()) {
            copy.deleteStudyById(TEST_STUDY)
            assert noTestStudyInDb()
        }
    }

    boolean noTestStudyInDb() {
        getTestStudyDbIdentifier() == null
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

    List readFieldsFromDb(Table table, String field, String where = '') {
        readFieldsFromDb(table, [field], where).collect { it[field] }
    }

    List<Map> readFieldsFromDb(Table table, List<String> selectFields, String where = '') {
        copy.database.jdbcTemplate.queryForList("SELECT ${selectFields.join(', ')} FROM ${table}" + (where ? ' ' + where : ''))
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
