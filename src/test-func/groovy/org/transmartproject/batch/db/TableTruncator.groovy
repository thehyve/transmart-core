package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Truncates table first by trying TRUCATE and then falling back to unbounded
 * DELETEs.
 */
@Slf4j
class TableTruncator {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    void truncate(String table, boolean cascade = true) {
        try {
            log.info "About to truncate table $table"
            jdbcTemplate.update("TRUNCATE TABLE ${table}${cascade ? ' CASCADE' : ''}", [:])
        } catch (DataIntegrityViolationException sqlException) {
            // cannot truncate due to foreign keys
            log.warn "Truncation of $table failed. Trying delete..."
            try {
                jdbcTemplate.update("DELETE FROM $table", [:])
                log.info "Delete statement on table $table succeeded"
            } catch (DataAccessException e) {
                log.error "Delete statement on table $table failed: ${e.message}"
                throw e
            }
        } catch (DataAccessException e) {
            // failed for another reason
            log.error "Truncation of table $table failed: ${e.message}"
            throw e
        }
    }

    void truncate(Iterable<String> tables, boolean cascade = true) {
        tables.each {
            truncate it, cascade
        }
    }
}
