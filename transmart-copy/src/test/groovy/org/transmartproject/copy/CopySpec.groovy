package org.transmartproject.copy

import org.transmartproject.copy.table.Concepts
import org.transmartproject.copy.table.Modifiers
import org.transmartproject.copy.table.Observations
import org.transmartproject.copy.table.Patients
import org.transmartproject.copy.table.Studies
import org.transmartproject.copy.table.TreeNodes
import spock.lang.Specification

class CopySpec extends Specification {

    static String TEST_STUDY = 'SURVEY0'
    static STUDY_FOLDER = './src/main/resources/examples/' + TEST_STUDY

    def studySpecificTables = [
            Observations.table,
            Studies.trial_visit_table,
            Studies.study_table,
            Studies.study_dimensions_table,
    ]

    static defaultConfig = new Copy.Config(
            dropIndexes: false,
            unlogged: false,
            write: false
    )

    def 'test loading the study data'() {
        given: 'Test database is available, the study is not loaded'

        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        if (TEST_STUDY in studyIds) {
            copy.deleteStudy(TEST_STUDY)
            studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        }
        assert !(TEST_STUDY in studyIds)
        Map beforeCounts = count(copy.database, studySpecificTables)
        Map fileRowCounts = count(STUDY_FOLDER, studySpecificTables)
        def expectedConceptPaths = readFieldsFromFile(STUDY_FOLDER, Concepts.table, 'concept_path')
        def expectedModifierPaths = readFieldsFromFile(STUDY_FOLDER, Modifiers.table, 'modifier_path')
        def expectedSubjectIds = readFieldsFromFile(STUDY_FOLDER, Patients.patient_mapping_table, 'patient_ide')
        def expectedTreeNodePaths = readFieldsFromFile(STUDY_FOLDER, TreeNodes.table, 'c_fullname')

        when: 'Loading example study data'
        copy.run(STUDY_FOLDER, defaultConfig)

        studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        Map afterCounts = count(copy.database, studySpecificTables)
        Map expectedCounts = fileRowCounts
                .collectEntries { Table table, Number rows -> [table, beforeCounts[table] + rows] }
        def inDbConceptPaths = readFieldsFromDb(copy.database, Concepts.table, 'concept_path')
        def inDbModifierPaths = readFieldsFromDb(copy.database, Modifiers.table, 'modifier_path')
        def inDbSubjectIds = readFieldsFromDb(copy.database, Patients.patient_mapping_table, 'patient_ide')
        def inDbTreeNodePaths = readFieldsFromDb(copy.database, TreeNodes.table, 'c_fullname')


        then: 'Expect the study to be loaded'
        TEST_STUDY in studyIds
        expectedCounts == afterCounts
        inDbConceptPaths.containsAll(expectedConceptPaths)
        inDbModifierPaths.containsAll(expectedModifierPaths)
        inDbSubjectIds.containsAll(expectedSubjectIds)
        inDbTreeNodePaths.containsAll(expectedTreeNodePaths)
    }

    def 'test deleting the study'() {
        given: 'Test database is available, the study is loaded'
        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        if (!(TEST_STUDY in studyIds)) {
            copy.run(STUDY_FOLDER, defaultConfig)
            studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        }
        assert TEST_STUDY in studyIds
        Map beforeCounts = count(copy.database, studySpecificTables)
        Map fileRowCounts = count(STUDY_FOLDER, studySpecificTables)

        when: 'Deleting the study'
        copy.deleteStudy(TEST_STUDY)

        studyIds = readFieldsFromDb(copy.database, Studies.study_table, 'study_id')
        Map afterCounts = count(copy.database, studySpecificTables)
        Map expectedCounts = fileRowCounts
                .collectEntries { Table table, Number rows -> [table, beforeCounts[table] - rows] }

        then: 'Expect the study to be loaded'
        !(TEST_STUDY in studyIds)
        expectedCounts == afterCounts
    }

    def 'test detecting if table exists'() {
        given: 'Test database is available'

        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed

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

    Map<Table, Number> count(Database database, Iterable<Table> tables) {
        tables.collectEntries { Table table ->
            [
                    table,
                    database.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ${table}", new Object[0], Long)
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

    List readFieldsFromDb(Database database, Table table, String field, String where = '') {
        readFieldsFromDb(database, table, [field], where).collect { it[field] }
    }

    List<Map> readFieldsFromDb(Database database, Table table, List<String> selectFields, String where = '') {
        database.jdbcTemplate.queryForList("SELECT ${selectFields.join(', ')} FROM ${table}" + (where ? '' + where : ''))
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
                    headerIndex = selectFields.collectEntries { String selectField -> [selectField, row.findIndexOf { it == selectField }] }
                } else {
                    result << headerIndex.collectEntries { header, column -> [header, row[column]] }
                }
            }
        }
        result
    }
}
