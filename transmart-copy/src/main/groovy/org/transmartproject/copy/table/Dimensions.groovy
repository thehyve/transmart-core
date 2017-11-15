/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.RowCallbackHandler
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util
import org.transmartproject.copy.exception.InvalidInput

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@CompileStatic
class Dimensions {

    static final Table table = new Table('i2b2metadata', 'dimension_description')

    final Database database

    final LinkedHashMap<String, Class> columns

    final Map<String, Long> dimensionNameToId = [:]
    final List<Long> indexToDimensionId = []

    Dimensions(Database database) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
    }

    @CompileStatic
    static class DimensionRowHandler implements RowCallbackHandler {
        final Map<String, Long> dimensionNameToId = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def id = rs.getLong('id')
            def name = rs.getString('name')
            dimensionNameToId[name] = id
        }
    }

    void fetch() {
        def dimensionHandler = new DimensionRowHandler()
        database.jdbcTemplate.query(
                "select id, name from ${table}".toString(),
                dimensionHandler
        )
        dimensionNameToId.putAll(dimensionHandler.dimensionNameToId)
        log.info "Dimensions loaded: ${dimensionNameToId.size()} entries."
        log.debug "Entries: ${dimensionNameToId.toMapString()}"
    }

    void load(String rootPath) {
        def dimensionsFile = new File(rootPath, table.fileName)
        dimensionsFile.withReader { reader ->
            log.info "Reading dimensions from file ..."
            def tx = database.beginTransaction()
            def insertCount = 0
            def existingCount = 0
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(table.fileName, data, columns)
                    return
                }
                try {
                    def dimensionData = Util.asMap(columns, data)
                    def dimensionIndex = dimensionData['id'] as long
                    if (i != dimensionIndex + 1) {
                        throw new InvalidInput("The dimensions are not in order. (Found ${dimensionIndex} on line ${i}.)")
                    }
                    def dimensionName = dimensionData['name'] as String
                    def dimensionId = dimensionNameToId[dimensionName]
                    if (dimensionId) {
                        existingCount++
                    } else {
                        insertCount++
                        log.info "Inserting dimension: ${dimensionName}."
                        dimensionId = database.insertEntry(table, columns, 'id', dimensionData)
                        log.debug "Dimension inserted [id: ${dimensionId}]."
                    }
                    indexToDimensionId.add(dimensionId)
                    log.debug "Registered dimension at index ${dimensionIndex}: ${dimensionName} [${dimensionId}]."
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${table.fileName}: ${e.message}."
                    throw e
                }
            }
            database.commit(tx)
            log.info "${existingCount} existing dimensions found."
            log.info "${insertCount} dimensions inserted."
        }
    }

}
