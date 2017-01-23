package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.transmartproject.batch.beans.Postgresql

/**
 * Truncates table first by trying TRUCATE and then falling back to unbounded
 * DELETEs.
 */
@Slf4j
@Postgresql
class PostgresTableTruncator extends TableTruncator {

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

}
