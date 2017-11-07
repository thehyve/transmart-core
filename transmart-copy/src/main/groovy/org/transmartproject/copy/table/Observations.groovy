/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.copy.Database
import org.transmartproject.copy.Util
import org.transmartproject.copy.exception.InvalidInput

@Slf4j
@CompileStatic
class Observations {

    static final String table = 'i2b2demodata.observation_fact'
    static final String observations_file = 'i2b2demodata/observation_fact.tsv'

    final Database database

    final LinkedHashMap<String, Class> columns
    final int patientNumIndex
    final int trialVisitNumIndex
    final int conceptCodeIndex
    final int instanceNumIndex

    final Studies studies
    final Concepts concepts
    final Patients patients

    Observations(Database database, Studies studies, Concepts concepts, Patients patients) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
        this.patientNumIndex = columns.collect { it.key }.indexOf('patient_num')
        this.trialVisitNumIndex = columns.collect { it.key }.indexOf('trial_visit_num')
        this.conceptCodeIndex = columns.collect { it.key }.indexOf('concept_cd')
        this.instanceNumIndex = columns.collect { it.key }.indexOf('instance_num')
        this.studies = studies
        this.concepts = concepts
        this.patients = patients
    }

    void checkFiles(String rootPath) {
        File inputFile = new File(rootPath, observations_file)
        if (!inputFile.exists()) {
            throw new InvalidInput("No file ${observations_file}")
        }
    }

    int getMaxInstanceNum() {
        database.sql.firstRow(
                "select max(instance_num) as max_instance_num from ${table}".toString(),
        )['max_instance_num'] as int
    }

    String[] transformRow(final String[] data, final int baseInstanceNum) {
        String[] result = Arrays.copyOf(data, data.length)
        // replace patient index with patient num
        def patientIndex = data[patientNumIndex] as int
        if (patientIndex >= patients.indexToPatientNum.size()) {
            throw new InvalidInput("Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        result[patientNumIndex] = patients.indexToPatientNum[patientIndex]
        def trialVisitIndex = data[trialVisitNumIndex] as int
        if (trialVisitIndex >= studies.indexToTrialVisitNum.size()) {
            throw new InvalidInput("Trial visit index higher than the number of trial visits (${studies.indexToTrialVisitNum.size()})")
        }
        result[trialVisitNumIndex] = studies.indexToTrialVisitNum[trialVisitIndex]
        def conceptCode = data[conceptCodeIndex] as String
        if (!(conceptCode in concepts.conceptCodes)) {
            throw new InvalidInput("Unknown concept code: ${conceptCode}")
        }
        def instanceIndex = data[instanceNumIndex] as int
        result[instanceNumIndex] = baseInstanceNum + instanceIndex
        result
    }

    void load(String rootPath, File tmpDir) {
        File inputFile = new File(rootPath, observations_file)
        File tempFile = File.createTempFile('observation_fact_', '.tsv', tmpDir)
        // Transform data: replace patient index with patient num,
        // and write transformed data to temporary file.
        log.info 'Reading and transforming observations data ...'
        log.info "Writing to temporary file ${tempFile.path} ..."
        int n = 0
        int baseInstanceNum = maxInstanceNum + 1
        log.info "Using ${baseInstanceNum} as lowest instance num."
        inputFile.withReader { reader ->
            tempFile.withPrintWriter { writer ->
                def tsvReader = Util.tsvReader(reader)
                def tsvWriter = Util.tsvWriter(writer)
                tsvReader.eachWithIndex { String[] data, int i ->
                    if (i == 0) {
                        Util.verifyHeader(tempFile.path, data, columns)
                        return
                    }
                    n = i
                    try {
                        if (columns.size() != data.length) {
                            throw new InvalidInput("Data row length (${data.length}) does not match number of columns (${columns.size()}).")
                        }
                        String[] result = transformRow(data, baseInstanceNum)
                        tsvWriter.writeNext(result)
                        if (i % 1000 == 0) {
                            log.info "${i} rows read ..."
                            tsvWriter.flush()
                        }
                    } catch (Exception e) {
                        log.error "Error processing row ${i} of ${observations_file}", e
                    }
                }
                tsvWriter.flush()
            }
        }
        log.info "Done reading and transforming observations data. ${n} rows read."
        // Insert transformed data
        log.info 'Loading observations data into the database ...'
        database.copyFile(table, tempFile)
        log.info 'Done loading observations data.'
    }

}
