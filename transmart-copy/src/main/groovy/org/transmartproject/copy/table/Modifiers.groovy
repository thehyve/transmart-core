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
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@CompileStatic
class Modifiers {

    static final String table = 'i2b2demodata.modifier_dimension'
    static final String modifiers_file = 'i2b2demodata/modifier_dimension.tsv'

    final Database database

    final LinkedHashMap<String, Class> columns

    final Set<String> modifierCodes = []

    Modifiers(Database database) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
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
                "select modifier_cd from ${table}".toString(),
                modifierHandler
        )
        modifierCodes.addAll(modifierHandler.modifierCodes)
        log.info "Modifier codes loaded: ${modifierCodes.size()}."
        log.debug "Modifier codes: ${modifierCodes}"
    }

    void load(String rootPath) {
        def modifiersFile = new File(rootPath, modifiers_file)
        if (!modifiersFile.exists()) {
            log.info "Skip loading of modifiers. No file ${modifiers_file} found."
            return
        }
        modifiersFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(modifiers_file, data, columns)
                    return
                }
                try {
                    def modifierData = Util.asMap(columns, data)
                    def modifierCode = modifierData['modifier_cd'] as String
                    if (modifierCode in modifierCodes) {
                        log.info "Found existing modifier: ${modifierCode}."
                    } else {
                        log.info "Inserting new modifier: ${modifierCode} ..."
                        database.insertEntry(table, columns, modifierData)
                        modifierCodes.add(modifierCode)
                    }
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${modifiers_file}: ${e.message}."
                    throw e
                }
            }
        }
    }

}
