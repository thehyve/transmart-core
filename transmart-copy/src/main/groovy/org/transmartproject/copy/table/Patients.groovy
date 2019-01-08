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

/**
 * Fetching and loading of subjects.
 */
@Slf4j
@CompileStatic
class Patients {

    static final Table PATIENT_DIMENSION_TABLE = new Table('i2b2demodata', 'patient_dimension')
    static final Table PATIENT_MAPPING_TABLE = new Table('i2b2demodata', 'patient_mapping')

    final Database database

    final LinkedHashMap<String, Class> patientDimensionColumns
    final LinkedHashMap<String, Class> patientMappingColumns

    final Map<String, Long> subjectIdToPatientNum = [:]
    final Map<Long, Long> indexToPatientNum = [:]

    Patients(Database database) {
        this.database = database
        this.patientDimensionColumns = this.database.getColumnMetadata(PATIENT_DIMENSION_TABLE)
        this.patientMappingColumns = this.database.getColumnMetadata(PATIENT_MAPPING_TABLE)
    }

    @CompileStatic
    static class PatientMappingRowHandler implements RowCallbackHandler {
        final Map<String, Long> subjectIdToPatientNum = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def patientIde = rs.getString('patient_ide')
            def patientIdeSource = rs.getString('patient_ide_source')
            Long patientNum = rs.getLong('patient_num')
            String key = "${patientIdeSource}:${patientIde}"
            subjectIdToPatientNum[key] = patientNum
        }
    }

    void fetch() {
        def patientMappingHandler = new PatientMappingRowHandler()
        database.jdbcTemplate.query(
                "select patient_ide, patient_ide_source, patient_num from ${PATIENT_MAPPING_TABLE}".toString(),
                patientMappingHandler
        )
        subjectIdToPatientNum.putAll(patientMappingHandler.subjectIdToPatientNum)
        log.info "Patient mapping entries in the database: ${subjectIdToPatientNum.size()}."
    }

    void load(String rootPath) {
        log.info "Reading patient data from files ..."
        def insertCount = 0
        def existingCount = 0
        Set<Long> missingPatients = []
        Map<Long, Map> missingPatientsMappingData = [:]
        LinkedHashMap<String, Class> patientMappingHeader = patientMappingColumns
        def mappingFile = new File(rootPath, PATIENT_MAPPING_TABLE.fileName)
        mappingFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    patientMappingHeader =
                            Util.verifyHeader(PATIENT_MAPPING_TABLE.fileName, data, patientMappingColumns)
                    return
                }
                try {
                    def patientMappingData = Util.asMap(patientMappingHeader, data)
                    Long patientIndex = ((BigDecimal) patientMappingData['patient_num']).longValueExact()
                    def patientIde = patientMappingData['patient_ide'] as String
                    def patientIdeSource = patientMappingData['patient_ide_source'] as String
                    def key = "${patientIdeSource}:${patientIde}".toString()
                    def patientNum = subjectIdToPatientNum[key]
                    if (patientNum) {
                        existingCount++
                        log.debug "Patient ${patientIndex} already present."
                        indexToPatientNum[patientIndex] = patientNum
                    } else {
                        missingPatients.add(patientIndex)
                        missingPatientsMappingData[patientIndex] = patientMappingData
                    }
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${PATIENT_MAPPING_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        def tx = database.beginTransaction()
        LinkedHashMap<String, Class> patientDimensionHeader = patientDimensionColumns
        def patientsFile = new File(rootPath, PATIENT_DIMENSION_TABLE.fileName)
        patientsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    patientDimensionHeader =
                            Util.verifyHeader(PATIENT_DIMENSION_TABLE.fileName, data, patientDimensionColumns)
                    return
                }
                try {
                    def patientData = Util.asMap(patientDimensionHeader, data)
                    Long patientIndex = ((BigDecimal) patientData['patient_num']).longValueExact()
                    if (patientIndex in missingPatients) {
                        insertCount++
                        Long patientNum = database.insertEntry(
                                PATIENT_DIMENSION_TABLE, patientDimensionHeader, 'patient_num', patientData)
                        log.debug "Patient inserted [patient_num: ${patientNum}]."
                        indexToPatientNum[patientIndex] = patientNum
                        Map<String, Object> patientMappingData = missingPatientsMappingData[patientIndex]
                        patientMappingData['patient_num'] = patientNum
                        database.insertEntry(PATIENT_MAPPING_TABLE, patientMappingHeader, patientMappingData)
                        log.debug "Patient mapping inserted [patient_num: ${patientNum}]."
                    }
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${PATIENT_DIMENSION_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        database.commit(tx)
        log.info "${existingCount} existing patients found."
        log.info "${insertCount} patients inserted."
    }

}
