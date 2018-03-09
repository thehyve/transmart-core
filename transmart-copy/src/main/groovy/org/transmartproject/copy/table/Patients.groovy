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
class Patients {

    static final Table patient_dimension_table = new Table('i2b2demodata', 'patient_dimension')
    static final Table patient_mapping_table = new Table('i2b2demodata', 'patient_mapping')

    final Database database

    final LinkedHashMap<String, Class> patient_dimension_columns
    final LinkedHashMap<String, Class> patient_mapping_columns

    final Map<String, Long> subjectIdToPatientNum = [:]
    final Map<Integer, Long> indexToPatientNum = [:]
    final List<String> indexToSubjectId = []

    Patients(Database database) {
        this.database = database
        this.patient_dimension_columns = this.database.getColumnMetadata(patient_dimension_table)
        this.patient_mapping_columns = this.database.getColumnMetadata(patient_mapping_table)
    }

    @CompileStatic
    static class PatientMappingRowHandler implements RowCallbackHandler {
        final Map<String, Long> subjectIdToPatientNum = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def patientIde = rs.getString('patient_ide')
            def patientIdeSource = rs.getString('patient_ide_source')
            def patientNum = rs.getLong('patient_num')
            def key = "${patientIdeSource}:${patientIde}".toString()
            subjectIdToPatientNum[key] = patientNum
        }
    }

    void fetch() {
        def patientMappingHandler = new PatientMappingRowHandler()
        database.jdbcTemplate.query(
                "select patient_ide, patient_ide_source, patient_num from ${patient_mapping_table}".toString(),
                patientMappingHandler
        )
        subjectIdToPatientNum.putAll(patientMappingHandler.subjectIdToPatientNum)
        log.info "Patient mapping entries in the database: ${subjectIdToPatientNum.size()}."
    }

    void load(String rootPath) {
        log.info "Reading patient data from files ..."
        def insertCount = 0
        def existingCount = 0
        Set<Integer> missingPatients = []
        Map<Integer, Map> missingPatientsMappingData = [:]
        LinkedHashMap<String, Class> patient_mapping_header = patient_mapping_columns
        def mappingFile = new File(rootPath, patient_mapping_table.fileName)
        mappingFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    patient_mapping_header = Util.verifyHeader(patient_mapping_table.fileName, data, patient_mapping_columns)
                    return
                }
                try {
                    def patientMappingData = Util.asMap(patient_mapping_header, data)
                    def patientIndex = patientMappingData['patient_num'] as int
                    if (i != patientIndex + 1) {
                        throw new IllegalStateException("The patients in the patient mapping are not in order. (Found ${patientIndex} on line ${i}.)")
                    }
                    def patientIde = patientMappingData['patient_ide'] as String
                    def patientIdeSource = patientMappingData['patient_ide_source'] as String
                    def key = "${patientIdeSource}:${patientIde}".toString()
                    indexToSubjectId.add(key)
                    def patientNum = subjectIdToPatientNum[key]
                    if (patientNum) {
                        existingCount++
                        log.debug "Patient ${patientIndex} already present."
                        indexToPatientNum[patientIndex] = patientNum
                    } else {
                        missingPatients.add(patientIndex)
                        missingPatientsMappingData[patientIndex] = patientMappingData
                    }
                } catch (Exception e) {
                    log.error "Error on line ${i} of ${patient_mapping_table.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        def tx = database.beginTransaction()
        LinkedHashMap<String, Class> patient_dimension_header = patient_dimension_columns
        def patientsFile = new File(rootPath, patient_dimension_table.fileName)
        patientsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    patient_dimension_header = Util.verifyHeader(patient_dimension_table.fileName, data, patient_dimension_columns)
                    return
                }
                try {
                    def patientData = Util.asMap(patient_dimension_header, data)
                    def patientIndex = patientData['patient_num'] as int
                    if (i != patientIndex + 1) {
                        throw new IllegalStateException("The patients are not in order. (Found ${patientIndex} on line ${i}.)")
                    }
                    if (patientIndex in missingPatients) {
                        insertCount++
                        Long patientNum = database.insertEntry(patient_dimension_table, patient_dimension_header, 'patient_num', patientData)
                        log.debug "Patient inserted [patient_num: ${patientNum}]."
                        indexToPatientNum[patientIndex] = patientNum
                        def patientMappingData = missingPatientsMappingData[patientIndex]
                        patientMappingData['patient_num'] = patientNum
                        database.insertEntry(patient_mapping_table, patient_mapping_header, patientMappingData)
                        log.debug "Patient mapping inserted [patient_num: ${patientNum}]."
                    }
                } catch (Exception e) {
                    log.error "Error on line ${i} of ${patient_dimension_table.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        database.commit(tx)
        log.info "${existingCount} existing patients found."
        log.info "${insertCount} patients inserted."
    }

}
