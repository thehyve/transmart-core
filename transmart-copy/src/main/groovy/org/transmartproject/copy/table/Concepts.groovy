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

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@CompileStatic
class Concepts {

    static final Table table = new Table('i2b2demodata', 'concept_dimension')

    final Database database

    final LinkedHashMap<String, Class> columns

    final Set<String> conceptCodes = []
    final Set<String> conceptPaths = []
    final Map<String, String> conceptCodeToConceptPath = [:]
    /**
     * Workaround. Stores old to new concept path mapping to be able to update tree nodes on later step.
     */
    final Map<String, String> oldToNewConceptPath = [:]
    final boolean updateConceptPath

    Concepts(Database database, updateConceptPath = false) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
        this.updateConceptPath = updateConceptPath
    }

    @CompileStatic
    static class ConceptRowHandler implements RowCallbackHandler {
        final List<String> conceptCodes = []
        final List<String> conceptPaths = []
        final Map<String, String> conceptCodeToConceptPath = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def conceptCode = rs.getString('concept_cd')
            def conceptPath = rs.getString('concept_path')
            def codeInserted = conceptCodes << conceptCode
            conceptPaths << conceptPath
            if (!codeInserted) {
                log.warn "Duplicate concept code in the database: ${conceptCode}"
                def knownPath = conceptCodeToConceptPath[conceptCode]
                if (knownPath != conceptPath) {
                    log.warn "Inconsistent data in the database: concept code ${conceptCode} associated with multiple paths:"
                    log.warn " - ${knownPath}"
                    log.warn " - ${conceptPath}"
                }
            }
            conceptCodeToConceptPath[conceptCode] = conceptPath
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
        conceptCodeToConceptPath.putAll(rowHandler.conceptCodeToConceptPath)
        log.info "Concepts in the database: ${conceptCodes.size()}."
        log.debug "Concept codes: ${conceptCodes}"
    }

    void load(String rootPath) {
        def conceptsFile = new File(rootPath, table.fileName)
        def tx = database.beginTransaction()
        conceptsFile.withReader { reader ->
            log.info "Reading concepts from file ..."
            def insertCount = 0
            def updatedCount = 0
            def existingCount = 0
            def tsvReader = Util.tsvReader(reader)
            LinkedHashMap<String, Class> header = columns
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(table.fileName, data, columns)
                    return
                }
                try {
                    def conceptData = Util.asMap(header, data)
                    def conceptCode = conceptData['concept_cd'] as String
                    def conceptPath = conceptData['concept_path'] as String
                    if (conceptCode in conceptCodes) {
                        if (conceptPath != conceptCodeToConceptPath[conceptCode]) {
                            if (updateConceptPath) {
                                log.info("Updating concept path from '${conceptCodeToConceptPath[conceptCode]}' to '${conceptPath}'.")
                                int records = database.namedParameterJdbcTemplate.update(
                                        "delete from ${table} where concept_cd = :conceptCode and concept_path = :conceptPath",
                                        [conceptPath: conceptCodeToConceptPath[conceptCode],
                                         conceptCode: conceptCode])
                                conceptPaths.remove(conceptCodeToConceptPath[conceptCode])
                                log.debug("${records} records with '${conceptCodeToConceptPath[conceptCode]}' concept path were removed. Inserting '${conceptPath}' instead.")
                                database.insertEntry(table, header, conceptData)
                                conceptPaths.add(conceptPath)
                                oldToNewConceptPath[conceptCodeToConceptPath[conceptCode]] = conceptPath
                                conceptCodeToConceptPath[conceptCode] = conceptPath
                                updatedCount++
                            } else {
                                log.error "Error: trying to load concept with code ${conceptCode} and path ${conceptPath},"
                                log.error "but concept with that code already exists with path ${conceptCodeToConceptPath[conceptCode]}."
                                throw new IllegalStateException("Cannot load concept with code ${conceptCode}. Other concept already exists with that code.")
                            }
                        } else {
                            existingCount++
                            log.debug "Found existing concept: ${conceptCode}."
                        }
                    } else if (conceptPath in conceptPaths) {
                        log.error "Error: trying to load concept with code ${conceptCode} and path ${conceptPath},"
                        log.error "but concept with that path already exists with another code."
                        throw new IllegalStateException("Cannot load concept with code ${conceptCode}. Other concept already exists with that code.")
                    } else {
                        insertCount++
                        log.debug "Inserting new concept: ${conceptCode} ..."
                        database.insertEntry(table, header, conceptData)
                        conceptCodes.add(conceptCode)
                        conceptPaths.add(conceptPath)
                        conceptCodeToConceptPath[conceptCode] = conceptPath
                    }
                } catch (Exception e) {
                    log.error "Error on line ${i} of ${table.fileName}: ${e.message}"
                    throw e
                }
            }
            database.commit(tx)
            log.info "${existingCount} existing concepts found."
            log.info "${insertCount} concepts inserted."
            log.info "${updatedCount} concepts updated."
        }
    }

}
