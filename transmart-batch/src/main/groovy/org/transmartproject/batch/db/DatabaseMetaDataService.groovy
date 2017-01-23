package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import org.transmartproject.batch.support.StringUtils

import javax.sql.DataSource
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Service to retrieve meta information about the database, its tables, columns.
 */
@Component
class DatabaseMetaDataService {

    @Autowired
    DataSource dataSource

    private String getIdentifier(DatabaseMetaData meta, String name) {
        if (meta.storesUpperCaseIdentifiers()) {
            name.toUpperCase()
        } else if (meta.storesLowerCaseIdentifiers()) {
            name.toLowerCase()
        } else {
            name
        }
    }

    /**
     * Get column meta information.
     * @param spec
     * @return Map contains following fields [maxSize: <num>, nullable: <true/false>]
     */
    def getColumnDeclaration(ColumnSpecification spec) {
        def connection
        try {
            connection = DataSourceUtils.getConnection(dataSource)

            def meta = connection.metaData
            String escSymbol = meta.searchStringEscape
            ResultSet rs = meta.getColumns(
                    null,
                    StringUtils.escapeForLike(getIdentifier(meta, spec.schema), escSymbol),
                    StringUtils.escapeForLike(getIdentifier(meta, spec.table), escSymbol),
                    StringUtils.escapeForLike(getIdentifier(meta, spec.column), escSymbol))

            if (rs.next()) {
                return [
                    maxSize : rs.getInt('COLUMN_SIZE'), // in UTF-16 code units
                    // will have to be improved for oracle
                    nullable: rs.getInt('NULLABLE') as boolean,
                ]
            }

        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }
}
