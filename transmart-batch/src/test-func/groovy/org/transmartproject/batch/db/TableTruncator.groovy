package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Created by ruslan on 11/13/15.
 */
abstract class TableTruncator {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    abstract void truncate(String table, boolean cascade)

    void truncate(Iterable<String> tables, boolean cascade = true) {
        tables.each {
            truncate it, cascade
        }
    }

}
