package org.transmartproject.copy

import org.transmartproject.copy.table.Studies
import spock.lang.Specification

class CopySpec extends Specification {

    def 'test loading SURVEY0 data'() {
        def survey_study_query = "select * from ${Studies.study_table} where study_id = 'SURVEY0'".toString()
        given: 'Test database is available, study SURVEY0 is not loaded'
        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }
        if (!studies.empty) {
            copy.deleteStudy('SURVEY0')
            studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }
        }
        assert studies.empty

        when: 'Loading example study data'
        copy.run('./src/main/resources/examples/SURVEY0')
        studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }

        then: 'Expect the study to be loaded'
        studies.size() == 1
    }

    def 'test deleting SURVEY0'() {
        def survey_study_query = "select * from ${Studies.study_table} where study_id = 'SURVEY0'".toString()
        given: 'Test database is available, study SURVEY0 loaded'
        def copy = new Copy()
        copy.init()
        assert !copy.database.connection.closed
        def studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }
        if (studies.empty) {
            copy.run('./src/main/resources/examples/SURVEY0')
            studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }
        }
        assert studies.size() == 1

        when: 'Deleting study SURVEY0'
        copy.deleteStudy('SURVEY0')
        studies = copy.database.sql.rows(survey_study_query).collect { it['study_id'] as String }

        then: 'Expect the study to be loaded'
        studies.empty
    }

}
