package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert

import javax.annotation.PostConstruct

/**
 *
 */
class ObservationFactTableWriter implements ItemWriter<FactRowSet> {

    @Autowired
    private JdbcTemplate jdbcTemplate

    private SimpleJdbcInsert insert

    @Override
    void write(List<? extends FactRowSet> items) throws Exception {

        List<Map<String,Object[]>> rows = []

        items.each {
            rows.addAll(it.observationFactRows)
        }

        Map<String,Object>[] array = rows as Map<String,Object>[]
        insert.executeBatch(array)
    }

    @PostConstruct
    void init() {
        insert = new SimpleJdbcInsert(jdbcTemplate)
        insert.withSchemaName('i2b2demodata')
        insert.withTableName('observation_fact')
    }
}
