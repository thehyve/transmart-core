/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.RowCallbackHandler
import org.transmartproject.copy.ColumnMetadata
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Fetching and loading of custom dimensions as modifiers.
 */
@Slf4j
@CompileStatic
class Modifiers {

    static final Table TABLE = new Table('i2b2demodata', 'modifier_dimension')

    final Database database

    final LinkedHashMap<String, ColumnMetadata> columns

    final Set<String> modifierCodes = []

    Modifiers(Database database) {
        this.database = database
        this.columns = this.database.getColumnMetadata(TABLE)
    }

    @CompileStatic
    static class ModifierRowHandler implements RowCallbackHandler {
        final List<String> modifierCodes = []

        @Override
        void processRow(ResultSet rs) throws SQLException {
            modifierCodes << rs.getString('modifier_cd')
        }
    }

    void fetch() {
        def modifierHandler = new ModifierRowHandler()
        database.jdbcTemplate.query(
                "select modifier_cd from ${TABLE}".toString(),
                modifierHandler
        )
        modifierCodes.addAll(modifierHandler.modifierCodes)
        log.info "Modifiers in the database: ${modifierCodes.size()}."
        log.debug "Modifier codes: ${modifierCodes}"
    }

    void load(String rootPath) {
        def modifiersFile = new File(rootPath, TABLE.fileName)
        if (!modifiersFile.exists()) {
            log.info "Skip loading of modifiers. No file ${TABLE.fileName} found."
            return
        }
        def tx = database.beginTransaction()
        LinkedHashMap<String, Class> header
        modifiersFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(TABLE.fileName, data, columns)
                    return
                }
                try {
                    def modifierData = Util.asMap(header, data)
                    def modifierCode = modifierData['modifier_cd'] as String
                    if (modifierCode in modifierCodes) {
                        log.info "Found existing modifier: ${modifierCode}."
                    } else {
                        log.info "Inserting new modifier: ${modifierCode} ..."
                        database.insertEntry(TABLE, header, modifierData)
                        modifierCodes.add(modifierCode)
                    }
                } catch(Throwable e) {
                    log.error "Error on line ${i} of ${TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        database.commit(tx)
    }

}
