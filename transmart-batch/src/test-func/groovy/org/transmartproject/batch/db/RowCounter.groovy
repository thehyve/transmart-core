package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Counts rows in a table, subject to an optional predicate.
 */
class RowCounter {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    long count(Map params, String table, String where = '') {
        String whereClause = where ? " WHERE $where" : ''
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM $table$whereClause",
                params, Long)
    }

    long count(String table, String where = '') {
        count([:], table, where)
    }
}
