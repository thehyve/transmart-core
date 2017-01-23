package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.PreparedStatementCallback
import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.clinical.db.objects.Tables

import java.sql.PreparedStatement

/**
 * Truncates table
 */
@Slf4j
@Oracle
class OracleTableTruncator extends TableTruncator {

    void truncate(String table, boolean cascade = true) {
        try {
            log.info "About to truncate table $table"
            if (cascade) {
                def schemaName = Tables.schemaName(table)
                def tableName = Tables.tableName(table)
                jdbcTemplate.execute('{CALL TM_CZ.DELETE_RECURSIVE(:owner, :tableName)}',
                        [
                                owner: schemaName,
                                tableName: tableName
                        ],
                        { PreparedStatement ps ->
                            ps.execute()
                        } as PreparedStatementCallback<Boolean>)
            } else {
                jdbcTemplate.update("DELETE FROM $table", [:])
            }
        } catch (DataAccessException e) {
            log.error "Could not truncate table $table: ${e.message}"
        }
    }

}
