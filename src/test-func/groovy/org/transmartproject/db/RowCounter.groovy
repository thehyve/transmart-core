package org.transmartproject.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class RowCounter {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    long count(Map params, String table, String where = '') {
        String whereClause = where ? " WHERE $where" : ''
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM $table$whereClause",
                params, Long)
    }
}
