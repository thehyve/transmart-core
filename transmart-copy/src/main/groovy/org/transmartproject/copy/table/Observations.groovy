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
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.Timestamp
import java.text.NumberFormat

@Slf4j
@CompileStatic
class Observations {

    static final Table temporaryTable = new Table(null, 'observation_fact_upload')
    static final Table table = new Table('i2b2demodata', 'observation_fact')

    static final Timestamp emptyDate = Timestamp.valueOf('0001-01-01 00:00:00')

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
        File inputFile = new File(rootPath, table.fileName)
        if (!inputFile.exists()) {
            throw new IllegalStateException("No file ${table.fileName}")
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
            throw new IllegalStateException("Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        row.patient_num = patients.indexToPatientNum[patientIndex]
        def trialVisitIndex = row.trial_visit_num as int
        if (trialVisitIndex >= studies.indexToTrialVisitNum.size()) {
            throw new IllegalStateException("Trial visit index higher than the number of trial visits (${studies.indexToTrialVisitNum.size()})")
        }
        row.trial_visit_num = studies.indexToTrialVisitNum[trialVisitIndex]
        def conceptCode = row.concept_cd as String
        if (!(conceptCode in concepts.conceptCodes)) {
            throw new IllegalStateException("Unknown concept code: ${conceptCode}")
        }
        def instanceIndex = row.instance_num as int
        row.instance_num = baseInstanceNum + instanceIndex
        def startDate = row.start_date
        if (!startDate) {
            row.start_date = emptyDate
        }
    }

    static final Map<String, List> tableIndexes = [
            'idx_fact_patient_num': ['patient_num'],
            'idx_fact_concept': ['concept_cd'],
            'idx_fact_trial_visit_num': ['trial_visit_num'],
    ]

    static final Map<String, List> nonModifiersTableIndexes = [
            'observation_fact_pct_idx': ['patient_num', 'concept_cd', 'trial_visit_num'],
    ]

    void setLoggedMode(boolean logged) {
        def tx = database.beginTransaction()
        log.info "Set 'logged' on ${table} to ${logged}"
        database.jdbcTemplate.execute("alter table ${table} set ${logged ? 'logged' : 'unlogged'}")
        database.commit(tx)
    }

    void restoreTableIndexesIfNotExist() {
        def tx = database.beginTransaction()
        log.info "Restore indexes on ${table} ..."
        tableIndexes.each { name, columns ->
            database.jdbcTemplate.execute(composeCreateIndexSql(name, columns))
        }
        nonModifiersTableIndexes.each { name, columns ->
            database.jdbcTemplate.execute("${composeCreateIndexSql(name, columns)} where modifier_cd='@'")
        }
        database.commit(tx)
    }

    private static String composeCreateIndexSql(String name, Iterable<String> columns) {
        "create index if not exists ${name} on ${table} using btree (${columns.join(', ')})"
    }

    void dropTableIndexesIfExist() {
        def tx = database.beginTransaction()
        log.info "Drop indexes on ${table} ..."
        (tableIndexes.keySet() + nonModifiersTableIndexes.keySet()).each { name ->
            database.jdbcTemplate.execute("drop index if exists i2b2demodata.${name}")
        }
        database.commit(tx)
    }

    void prepareTemporaryTable() {
        log.info "Creating temporary table ${temporaryTable} ..."
        try {
            URL url = getClass().getClassLoader().getResource('sql/observation_fact_upload.sql')
            url.withReader { reader ->
                String uploadTableDefinition = reader.text
                database.executeCommand(uploadTableDefinition)
                def uploadTableColumns = this.database.getColumnMetadata(temporaryTable)
                assert !uploadTableColumns.empty
                log.info 'Temporary table created.'
            }
        } catch (Exception e) {
            log.error "Error creating table: ${e.message}", e
            throw e
        }
    }

    void load(String rootPath) {
        log.info "Determining instance num ..."
        int baseInstanceNum = baseInstanceNum
        log.info "Using ${baseInstanceNum} as lowest instance num."
        File observationsFile = new File(rootPath, table.fileName)

        // Count number of rows
        log.info "Counting number of rows ..."
        int rowCount = 0
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            while (tsvReader.readNext() != null) {
                rowCount++
            }
        }
        log.info "${NumberFormat.getInstance().format(rowCount > 0 ? rowCount - 1 : 0)} rows in ${table.fileName}."

        Writer writer
        CSVWriter tsvWriter
        if (config.write) {
            writer = new PrintWriter(config.outputFile)
            tsvWriter = Util.tsvWriter(writer)
        }

        // Transform data: replace patient index with patient num,
        // trial visit index with trial visit num and instance num index with instance num,
        // and write transformed data to database.
        def tx = database.beginTransaction()

        def insertTable = table
        if (config.temporaryTable) {
            prepareTemporaryTable()
            tx.flush()
            insertTable = temporaryTable
        }

        log.info 'Reading, transforming and writing observations data ...'
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            String[] data = tsvReader.readNext()
            if (data != null) {
                int i = 1
                LinkedHashMap<String, Class> header = Util.verifyHeader(insertTable.fileName, data, columns)
                def insert = database.getInserter(insertTable, header)
                if (config.temporaryTable) {
                    insert = insert.withoutTableColumnMetaDataAccess()
                }
                final progressBar = new ProgressBar("Insert into ${insertTable}", rowCount - 1)
                progressBar.start()
                ArrayList<Map> batch = []
                data = tsvReader.readNext()
                i++
                int batchCount = 0
                while (data != null) {
                    try {
                        if (header.size() != data.length) {
                            throw new IllegalStateException("Data row length (${data.length}) does not match number of columns (${header.size()}).")
                        }
                        def row = Util.asMap(header, data)
                        transformRow(row, baseInstanceNum)
                        progressBar.stepBy(1)
                        if (config.write) {
                            tsvWriter.writeNext(row.values()*.toString() as String[])
                        } else {
                            batch.add(row)
                            if (batch.size() == config.batchSize) {
                                batchCount++
                                insert.executeBatch(batch.toArray() as Map[])
                                if (config.flushSize > 0 && batchCount % config.flushSize == 0) {
                                    tx.flush()
                                }
                                batch = []
                            }
                        }
                    } catch (Exception e) {
                        progressBar.stop()
                        log.error "Error processing row ${i} of ${table.fileName}: ${e.message}"
                        throw e
                    }
                    data = tsvReader.readNext()
                    i++
                }
                if (batch.size() > 0) {
                    batchCount++
                    insert.executeBatch(batch.toArray() as Map[])
                }
                progressBar.stop()
                log.info "${batchCount} batches of ${config.batchSize} inserted."
                return
            }
        }

        if (config.temporaryTable) {
            // transfer data from temporary table to observations table
            def columnSpec = columns.collect { it.key }.join(', ')
            def command = "insert into ${table} (${columnSpec}) select ${columnSpec} from ${temporaryTable}"
            database.jdbcTemplate.execute(command)
            tx.flush()

            database.jdbcTemplate.execute("drop table ${temporaryTable}")
        }
        database.commit(tx)
        if (config.write) {
            tsvWriter.close()
        }
        log.info "Done loading observations data."
    }

}
