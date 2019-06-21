/*
 * Copyright (c) 2019  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.RowCallbackHandler
import org.transmartproject.copy.*

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Fetching and loading of patient encounters.
 */
@Slf4j
@CompileStatic
class Visits {

    static final Table VISIT_DIMENSION_TABLE = new Table('i2b2demodata', 'visit_dimension')
    static final Table ENCOUNTER_MAPPING_TABLE = new Table('i2b2demodata', 'encounter_mapping')

    final Database database

    final Patients patients

    final LinkedHashMap<String, ColumnMetadata> visitDimensionColumns
    final LinkedHashMap<String, ColumnMetadata> encounterMappingColumns

    final Map<String, Long> encounterIdToEncounterNum = [:]
    final Map<Integer, Long> indexToEncounterNum = [:]
    final List<String> indexToEncounterId = []

    Visits(Database database, Patients patients) {
        this.database = database
        this.visitDimensionColumns = this.database.getColumnMetadata(VISIT_DIMENSION_TABLE)
        this.encounterMappingColumns = this.database.getColumnMetadata(ENCOUNTER_MAPPING_TABLE)
        this.patients = patients
    }

    @CompileStatic
    static class EncounterMappingRowHandler implements RowCallbackHandler {
        final Map<String, Long> encounterIdToEncounterNum = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def encounterIde = rs.getString('encounter_ide')
            def encounterIdeSource = rs.getString('encounter_ide_source')
            Long encounterNum = rs.getLong('encounter_num')
            String key = "${encounterIdeSource}:${encounterIde}"
            encounterIdToEncounterNum[key] = encounterNum
        }
    }

    void fetch() {
        def encounterMappingHandler = new EncounterMappingRowHandler()
        database.jdbcTemplate.query(
                "select encounter_ide_source, encounter_ide, encounter_num from ${ENCOUNTER_MAPPING_TABLE}".toString(),
                encounterMappingHandler
        )
        encounterIdToEncounterNum.putAll(encounterMappingHandler.encounterIdToEncounterNum)
        log.info "Encounter mapping entries in the database: ${encounterIdToEncounterNum.size()}."
    }

    void transformRow(final Map<String, Object> row) {
        // replace patient index with patient num
        int patientIndex = ((BigDecimal) row.get('patient_num')).intValueExact()
        if (patientIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException(
                    "Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        row.put('patient_num', patients.indexToPatientNum[patientIndex])
    }

    void load(String rootPath) {
        def mappingFile = new File(rootPath, ENCOUNTER_MAPPING_TABLE.fileName)
        if (!mappingFile.exists()) {
            log.info "Skip loading of visits. No file ${ENCOUNTER_MAPPING_TABLE.fileName} found."
            return
        }
        def visitsFile = new File(rootPath, VISIT_DIMENSION_TABLE.fileName)
        if (!visitsFile.exists()) {
            log.info "Skip loading of visits. No file ${VISIT_DIMENSION_TABLE.fileName} found."
            return
        }
        log.info "Reading encounter data from files ..."
        def counts = new Counts()
        Set<Integer> missingEncounters = []
        Map<Integer, Map> missingEncountersMappingData = [:]
        LinkedHashMap<String, Class> encounterMappingHeader
        mappingFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    encounterMappingHeader =
                            Util.verifyHeader(ENCOUNTER_MAPPING_TABLE.fileName, data, encounterMappingColumns)
                    return
                }
                try {
                    def encounterMappingData = Util.asMap(encounterMappingHeader, data)
                    int encounterIndex = ((BigDecimal) encounterMappingData['encounter_num']).intValueExact()
                    if (i != encounterIndex + 1) {
                        throw new IllegalStateException(
                                "The encounters in the encounter mapping are not in order." +
                                        " (Found ${encounterIndex} on line ${i}.)")
                    }
                    def encounterIde = encounterMappingData['encounter_ide'] as String
                    def encounterIdeSource = encounterMappingData['encounter_ide_source'] as String
                    def key = "${encounterIdeSource}:${encounterIde}".toString()
                    indexToEncounterId.add(key)
                    def encounterNum = encounterIdToEncounterNum[key]
                    if (encounterNum) {
                        counts.existingCount++
                        log.debug "Encounter ${encounterIndex} already present."
                        indexToEncounterNum[encounterIndex] = encounterNum
                    } else {
                        missingEncounters.add(encounterIndex)
                        missingEncountersMappingData[encounterIndex] = encounterMappingData
                    }
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${ENCOUNTER_MAPPING_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }

        def tx = database.beginTransaction()
        visitsFile.withReader { reader ->
            log.info "Reading visits from file ..."
            def tsvReader = Util.tsvReader(reader)
            LinkedHashMap<String, Class> header
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(VISIT_DIMENSION_TABLE.fileName, data, visitDimensionColumns)
                    return
                }
                try {
                    def visitData = Util.asMap(header, data)
                    int encounterIndex = ((Long) visitData['encounter_num']).intValue()
                    if (i != encounterIndex + 1) {
                        throw new IllegalStateException(
                                "The visits are not in order. (Found ${encounterIndex} on line ${i}.)")
                    }
                    if (encounterIndex in missingEncounters) {
                        counts.insertCount++
                        transformRow(visitData)
                        Long encounterNum = database.insertEntry(
                                VISIT_DIMENSION_TABLE, header, 'encounter_num', visitData)
                        log.debug "Visit inserted [encounter_num: ${encounterNum}]."
                        indexToEncounterNum[encounterIndex] = encounterNum
                        def encounterMappingData = missingEncountersMappingData[encounterIndex]
                        encounterMappingData['encounter_num'] = encounterNum
                        database.insertEntry(ENCOUNTER_MAPPING_TABLE, encounterMappingHeader, encounterMappingData)
                        log.debug "Encounter mapping inserted [encounter_num: ${encounterNum}]."
                    }
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${VISIT_DIMENSION_TABLE.fileName}: ${e.message}"
                    throw e
                }
            }
            database.commit(tx)
            log.info "${counts.existingCount} existing visits found."
            log.info "${counts.insertCount} visits inserted."
        }
    }

}
