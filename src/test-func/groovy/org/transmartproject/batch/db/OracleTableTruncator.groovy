package org.transmartproject.batch.db

import groovy.util.logging.Slf4j
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.PreparedStatementCallback
import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.clinical.db.objects.Tables

import java.sql.PreparedStatement
import java.sql.SQLException

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
                        new PreparedStatementCallback<Boolean>() {
                            @Override
                            public Boolean doInPreparedStatement(PreparedStatement ps)
                                    throws SQLException, DataAccessException {
                                return ps.execute();
                            }
                        }
                )
            } else {
                jdbcTemplate.update("DELETE FROM $table", [:])
            }
        } catch (DataAccessException e) {
            log.error "Could not truncate table $table: ${e.message}"
        }
    }

}
