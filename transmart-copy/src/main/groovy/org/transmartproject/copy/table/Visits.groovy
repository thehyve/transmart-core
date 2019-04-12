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

    static final Table TABLE = new Table('i2b2demodata', 'visit_dimension')

    final Database database

    final Patients patients

    final LinkedHashMap<String, ColumnMetadata> columns

    final Map<Long, Set<Integer>> patientEncounters = [:]

    Visits(Database database, Patients patients) {
        this.database = database
        this.columns = this.database.getColumnMetadata(TABLE)
        this.patients = patients
    }

    @CompileStatic
    static class VisitRowHandler implements RowCallbackHandler {
        final Map<Long, Set<Integer>> patientEncounters = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            Long patientNum = rs.getLong('patient_num')
            int encounterIndex = rs.getLong('encounter_num').intValue()
            if (!patientEncounters.containsKey(patientNum)) {
                patientEncounters[patientNum] = [] as Set<Integer>
            }
            patientEncounters[patientNum].add(encounterIndex)
        }
    }

    void fetch() {
        def visitHandler = new VisitRowHandler()
        database.jdbcTemplate.query(
                "select patient_num, encounter_num from ${TABLE}".toString(),
                visitHandler
        )
        patientEncounters.putAll(visitHandler.patientEncounters)
        log.info "Visit entries in the database for patients: ${patientEncounters.size()}."
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

    private void loadVisitData(LinkedHashMap<String, Class> header, Map visitData, Counts counts) {
        transformRow(visitData)
        int encounterIndex = ((BigDecimal) visitData.get('encounter_num')).intValueExact()
        def patientNum = (Long)visitData.get('patient_num')

        if (!patientEncounters.containsKey(patientNum)) {
            patientEncounters[patientNum] = [] as Set<Integer>
        }

        if (patientEncounters[patientNum].contains(encounterIndex)) {
            counts.existingCount++
            log.debug "Visit (${encounterIndex}, ${patientNum}) already present."
        } else {
            counts.insertCount++
            log.debug "Inserting new visit: (${encounterIndex}, ${patientNum}) ..."
            database.insertEntry(TABLE, header, visitData)
            patientEncounters[patientNum].add(encounterIndex)
        }
    }

    void load(String rootPath) {
        def visitsFile = new File(rootPath, TABLE.fileName)
        if (!visitsFile.exists()) {
            log.info "Skip loading of visits. No file ${TABLE.fileName} found."
            return
        }
        def tx = database.beginTransaction()
        visitsFile.withReader { reader ->
            log.info "Reading visits from file ..."
            def counts = new Counts()
            def tsvReader = Util.tsvReader(reader)
            LinkedHashMap<String, Class> header
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(TABLE.fileName, data, columns)
                    return
                }
                try {
                    def visitData = Util.asMap(header, data)
                    loadVisitData(header, visitData, counts)
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${TABLE.fileName}: ${e.message}"
                    throw e
                }
            }
            database.commit(tx)
            log.info "${counts.insertCount} existing visits found."
            log.info "${counts.insertCount} visits inserted."
        }
    }

}
