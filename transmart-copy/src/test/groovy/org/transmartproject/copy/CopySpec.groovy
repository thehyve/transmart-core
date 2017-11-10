package org.transmartproject.copy

import org.transmartproject.copy.table.Studies
import spock.lang.Specification

class CopySpec extends Specification {

    static String testStudy = 'SURVEY0'
    static survey_study_query = "select * from ${Studies.study_table} where study_id = '${testStudy}'".toString()

    static defaultConfig = new Copy.Config(
            dropIndexes: false,
            unlogged: false,
            write: false
    )

    List<String> fetchStudies(Database database) {
        database.jdbcTemplate.queryForList(survey_study_query).collect { it['study_id'] as String }
    }

    def 'test loading SURVEY0 data'() {
        given: 'Test database is available, study SURVEY0 is not loaded'
        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studies = fetchStudies(copy.database)
        if (!studies.empty) {
            copy.deleteStudy(testStudy)
            studies = fetchStudies(copy.database)
        }
        assert studies.empty

        when: 'Loading example study data'
        copy.run('./src/main/resources/examples/SURVEY0', defaultConfig)
        studies = fetchStudies(copy.database)

        then: 'Expect the study to be loaded'
        studies.size() == 1
    }

    def 'test deleting SURVEY0'() {
        given: 'Test database is available, study SURVEY0 loaded'
        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studies = fetchStudies(copy.database)
        if (studies.empty) {
            copy.run('./src/main/resources/examples/SURVEY0', defaultConfig)
            studies = fetchStudies(copy.database)
        }
        assert studies.size() == 1

        when: 'Deleting study SURVEY0'
        copy.deleteStudy(testStudy)
        studies = fetchStudies(copy.database)

        then: 'Expect the study to be loaded'
        studies.empty
    }

}
