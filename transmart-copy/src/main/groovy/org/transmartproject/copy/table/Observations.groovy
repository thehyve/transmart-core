/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import com.opencsv.CSVWriter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import me.tongfei.progressbar.ProgressBar
import org.transmartproject.copy.Copy
import org.transmartproject.copy.Database
import org.transmartproject.copy.Util
import org.transmartproject.copy.exception.InvalidInput

@Slf4j
@CompileStatic
class Observations {

    static final String table = 'i2b2demodata.observation_fact'
    static final String observations_file = 'i2b2demodata/observation_fact.tsv'

    static final String emptyDate = '0001-01-01 00:00:00'

    final Database database
    final Copy.Config config

    final LinkedHashMap<String, Class> columns

    final Studies studies
    final Concepts concepts
    final Patients patients

    Observations(Database database, Studies studies, Concepts concepts, Patients patients, Copy.Config config) {
        this.database = database
        this.config = config
        this.columns = this.database.getColumnMetadata(table)
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

    Integer getMaxInstanceNum() {
        database.jdbcTemplate.queryForObject(
                "select max(instance_num) as max_instance_num from ${table}".toString(),
                Integer.class
        )
    }

    Integer getBaseInstanceNum() {
        (maxInstanceNum ?: 0) + 1
    }

    void transformRow(Map<String, Object> row, final int baseInstanceNum) {
        // replace patient index with patient num
        def patientIndex = row.patient_num as int
        if (patientIndex >= patients.indexToPatientNum.size()) {
            throw new InvalidInput("Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        row.patient_num = patients.indexToPatientNum[patientIndex]
        def trialVisitIndex = row.trial_visit_num as int
        if (trialVisitIndex >= studies.indexToTrialVisitNum.size()) {
            throw new InvalidInput("Trial visit index higher than the number of trial visits (${studies.indexToTrialVisitNum.size()})")
        }
        row.trial_visit_num = studies.indexToTrialVisitNum[trialVisitIndex]
        def conceptCode = row.concept_cd as String
        if (!(conceptCode in concepts.conceptCodes)) {
            throw new InvalidInput("Unknown concept code: ${conceptCode}")
        }
        def instanceIndex = row.instance_num as int
        row.instance_num = baseInstanceNum + instanceIndex
        def startDate = row.start_date
        if (!startDate) {
            row.start_date = emptyDate
        }
    }

    static final Map<String, List> tableIndexes = [
            'fact_modifier_patient': ['modifier_cd', 'patient_num'],
            'idx_fact_patient_num': ['patient_num'],
            'idx_fact_trial_visit_num': ['trial_visit_num'],
            'idx_fact_concept': ['concept_cd'],
            'idx_fact_cpe': ['concept_cd', 'patient_num', 'encounter_num']
    ]

    void restoreTableIndexes() {
        if (config.unlogged) {
            try {
                log.info "Set 'logged' on ${table} ..."
                database.executeCommand("alter table ${table} set logged")
            } catch (Exception e) {
                log.warn "Could not set 'logged' on database table: ${e.message}"
            }
        }
        if (config.dropIndexes) {
            log.info "Restore indexes on ${table} ..."
            tableIndexes.each { name, columns ->
                database.executeCommand("create if not exists index ${name} on ${table} using btree (${columns.join(', ')})")
            }
        }
    }

    void dropTableIndexes() {
        if (config.unlogged) {
            try {
                log.info "Set 'unlogged' on ${table} ..."
                database.executeCommand("alter table ${table} set unlogged")
            } catch (Exception e) {
                log.warn "Could not set 'unlogged' on database table: ${e.message}"
            }
        }
        if (config.dropIndexes) {
            log.info "Temporarily drop indexes on ${table} ..."
            tableIndexes.keySet().each { name ->
                database.executeCommand("drop if exists index i2b2demodata.${name}")
            }
        }
    }

    void load(String rootPath) {
        log.info "Determining instance num ..."
        int baseInstanceNum = baseInstanceNum
        log.info "Using ${baseInstanceNum} as lowest instance num."
        File observationsFile = new File(rootPath, observations_file)

        // Count number of rows
        int rowCount = 0
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            while (tsvReader.readNext() != null) {
                rowCount++
            }
        }

        dropTableIndexes()

        Writer writer
        CSVWriter tsvWriter
        if (config.write) {
            writer = new PrintWriter(config.outputFile)
            tsvWriter = Util.tsvWriter(writer)
        }

        // Transform data: replace patient index with patient num,
        // trial visit index with trial visit num and instance num index with instance num,
        // and write transformed data to database.
        log.info 'Reading, transforming and writing observations data ...'
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            String[] data = tsvReader.readNext()
            if (data != null) {
                int i = 1
                LinkedHashMap<String, Class> header = Util.verifyHeader(observations_file, data, columns)
                def insert = database.getInserter(table, header)
                final progressBar = new ProgressBar("Insert into ${table}", rowCount - 1)
                progressBar.start()
                ArrayList<Map> batch = []
                data = tsvReader.readNext()
                i++
                while (data != null) {
                    try {
                        if (header.size() != data.length) {
                            throw new InvalidInput("Data row length (${data.length}) does not match number of columns (${header.size()}).")
                        }
                        def row = Database.getValueMap(header, data)
                        transformRow(row, baseInstanceNum)
                        progressBar.stepBy(1)
                        if (config.write) {
                            tsvWriter.writeNext(row.values()*.toString() as String[])
                        } else {
                            batch.add(row)
                            if (batch.size() == Database.batchSize) {
                                insert.executeBatch(batch.toArray() as Map[])
                                batch = []
                            }
                        }
                    } catch (Exception e) {
                        progressBar.stop()
                        log.error "Error processing row ${i} of ${observations_file}: ${e.message}"
                        restoreTableIndexes()
                        throw e
                    }
                    data = tsvReader.readNext()
                    i++
                }
                if (batch.size() > 0) {
                    insert.executeBatch(batch.toArray() as Map[])
                }
                progressBar.stop()
                restoreTableIndexes()
                return
            }
        }
        if (config.write) {
            tsvWriter.close()
        }
        log.info "Done loading observations data."
    }

}
