package org.transmartproject.db.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.simple.SimpleJdbcInsert

import javax.annotation.PostConstruct
import javax.sql.DataSource

class DbDictionaryTablesLoader {

    @Autowired
    @Qualifier('dataSource')
    DataSource dataSource

    @PostConstruct
    void init() {
        new SimpleJdbcInsert(dataSource)
                .withSchemaName('i2b2metadata')
                .withTableName('dimension_description')
                .executeBatch(
                [name: 'study'],
                [name: 'concept'],
                [name: 'patient'],
                [name: 'visit'],
                [name: 'start time'],
                [name: 'end time'],
                [name: 'location'],
                [name: 'trial visit'],
                [name: 'provider'],
                [name: 'biomarker'],
                [name: 'assay'],
                [name: 'projection'],
                [name: 'sample_type',
                 density: 'DENSE', modifier_code: 'TNS:SMPL', value_type: 'T', packable: 'NOT_PACKABLE', size_cd: 'SMALL'],
                [name: 'original_variable',
                 density: 'DENSE', modifier_code: 'TRANSMART:ORIGINAL_VARIABLE', value_type: 'T', packable: 'NOT_PACKABLE', size_cd: 'SMALL'],
        )
    }
}
