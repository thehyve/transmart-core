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
class Concepts {

    static final String table = 'i2b2demodata.concept_dimension'
    static final String concepts_file = 'i2b2demodata/concept_dimension.tsv'

    final Database database

    final LinkedHashMap<String, Class> columns

    final Set<String> conceptCodes = []
    final Set<String> conceptPaths = []

    Concepts(Database database) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
    }

    @CompileStatic
    static class ConceptRowHandler implements RowCallbackHandler {
        final List<String> conceptCodes = []
        final List<String> conceptPaths = []

        @Override
        void processRow(ResultSet rs) throws SQLException {
            conceptCodes << rs.getString('concept_cd')
            conceptPaths << rs.getString('concept_path')
        }
    }

    void fetch() {
        def rowHandler = new ConceptRowHandler()
        database.jdbcTemplate.query(
                "select concept_path, concept_cd from ${table}".toString(),
                rowHandler
        )
        conceptPaths.addAll(rowHandler.conceptPaths)
        conceptCodes.addAll(rowHandler.conceptCodes)
        log.info "Concepts loaded: ${conceptCodes.size()}."
        log.debug "Concept codes: ${conceptCodes}"
    }

    void load(String rootPath) {
        def conceptsFile = new File(rootPath, concepts_file)
        def tx = database.beginTransaction()
        conceptsFile.withReader { reader ->
            log.info "Reading concepts from file ..."
            def insertCount = 0
            def existingCount = 0
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(concepts_file, data, columns)
                    return
                }
                try {
                    def conceptData = Util.asMap(columns, data)
                    def conceptCode = conceptData['concept_cd'] as String
                    def conceptPath = conceptData['concept_path'] as String
                    if (conceptCode in conceptCodes) {
                        existingCount++
                        log.debug "Found existing concept: ${conceptCode}."
                    } else {
                        insertCount++
                        log.debug "Inserting new concept: ${conceptCode} ..."
                        database.insertEntry(table, columns, conceptData)
                        conceptCodes.add(conceptCode)
                        conceptPaths.add(conceptPath)
                    }
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${concepts_file}: ${e.message}."
                    throw e
                }
            }
            database.commit(tx)
            log.info "${existingCount} existing concepts found."
            log.info "${insertCount} concepts inserted."
        }
    }

}
