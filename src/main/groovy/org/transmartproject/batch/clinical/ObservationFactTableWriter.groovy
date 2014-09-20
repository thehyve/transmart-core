package org.transmartproject.batch.clinical

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert

import javax.annotation.PostConstruct

/**
 * Database writer of observation facts, based on FactRowSets
 */
@Slf4j
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
        try {
            insert.executeBatch(array)
        } catch (DuplicateKeyException ex) {
            //this seems to indicate an invalid file (even in the case of multiple visit names)
            log.error("Another observation_fact exists for the same patient/concept, " +
                    "very likely with a different visit_name." +
                    "If you want to reconcile the patient/concept codes with their ids, " +
                    "please run again with logger '{}' on level DEBUG ", 'org.transmartproject.batch.clinical')
            throw ex
        }
    }

    @PostConstruct
    void initInsert() {
        insert = new SimpleJdbcInsert(jdbcTemplate)
        insert.withSchemaName('i2b2demodata')
        insert.withTableName('observation_fact')
    }
}
