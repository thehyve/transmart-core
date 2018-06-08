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
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.transaction.TransactionStatus
import org.transmartproject.copy.Copy
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.Timestamp
import java.text.NumberFormat

/**
 * Fetching and loading of observations.
 */
@Slf4j
@CompileStatic
class Observations {

    static final Table TABLE = new Table('i2b2demodata', 'observation_fact')

    static final Timestamp EMPTY_DATE = Timestamp.valueOf('0001-01-01 00:00:00')

    final Database database
    final Copy.Config config

    final LinkedHashMap<String, Class> columns

    final Studies studies
    final Concepts concepts
    final Patients patients

    private final Map<Long, Table> partitionToTable = [:]

    Observations(Database database, Studies studies, Concepts concepts, Patients patients, Copy.Config config) {
        this.database = database
        this.config = config
        this.columns = this.database.getColumnMetadata(TABLE)
        this.studies = studies
        this.concepts = concepts
        this.patients = patients
    }

    void checkFiles(String rootPath) {
        File inputFile = new File(rootPath, TABLE.fileName)
        if (!inputFile.exists()) {
            throw new IllegalStateException("No file ${TABLE.fileName}")
        }
    }

    Integer getMaxInstanceNum() {
        database.jdbcTemplate.queryForObject(
                "select max(instance_num) as max_instance_num from ${TABLE}".toString(),
                Integer
        )
    }

    int getBaseInstanceNum() {
        (maxInstanceNum ?: 0) + 1
    }

    void transformRow(final Map<String, Object> row, final int baseInstanceNum) {
        // replace patient index with patient num
        int patientIndex = ((BigDecimal) row.get('patient_num')).intValueExact()
        if (patientIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException(
                    "Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        row.put('patient_num', patients.indexToPatientNum[patientIndex])
        int trialVisitIndex = ((BigDecimal) row.get('trial_visit_num')).intValueExact()
        if (trialVisitIndex >= studies.indexToTrialVisitNum.size()) {
            throw new IllegalStateException(
                    "Trial visit index higher than the number of trial visits (${studies.indexToTrialVisitNum.size()})")
        }
        row.put('trial_visit_num', studies.indexToTrialVisitNum[trialVisitIndex])
        String conceptCode = (String) row.get('concept_cd')
        if (!(conceptCode in concepts.conceptCodes)) {
            throw new IllegalStateException("Unknown concept code: ${conceptCode}")
        }
        int instanceIndex = ((BigDecimal) row.get('instance_num')).intValueExact()
        row.put('instance_num', baseInstanceNum + instanceIndex)
        if (!row.get('start_date')) {
            row.put('start_date', EMPTY_DATE)
        }
    }

    static final Map<String, List<String>> PARENT_TABLE_INDEXES = [
            'idx_fact_patient_num'    : ['patient_num'],
            'idx_fact_concept'        : ['concept_cd'],
            'idx_fact_trial_visit_num': ['trial_visit_num'],
    ]

    static final Map<String, List<String>> CHILD_TABLE_INDEXES = [
            'idx_fact_patient_num': ['patient_num'],
            'idx_fact_concept'    : ['concept_cd'],
    ]

    static final Map<String, List<String>> NON_MODIFIERS_TABLE_INDEXES = [
            'observation_fact_pct_idx': ['patient_num', 'concept_cd', 'trial_visit_num'],
    ]

    void restoreTableIndexesIfNotExist() {
        createTableIndexesIfNotExistForTable(TABLE)
    }

    void dropTableIndexesIfExist() {
        def tx = database.beginTransaction()
        log.info "Drop indexes on ${TABLE} ..."
        (PARENT_TABLE_INDEXES.keySet() + NON_MODIFIERS_TABLE_INDEXES.keySet()).each { name ->
            database.jdbcTemplate.execute("drop index if exists i2b2demodata.${name}")
        }
        database.commit(tx)
    }

    private void loadRow(CSVWriter tsvWriter,
                 LinkedHashMap<String, Class> header,
                 Map<String, Object> row,
                 Integer batchCount,
                 ArrayList<Map> batch,
                 SimpleJdbcInsert insert,
                 TransactionStatus tx) {
        if (config.write) {
            tsvWriter.writeNext(row.values()*.toString() as String[])
        } else {
            batch.add(row)
            if (batch.size() == config.batchSize) {
                batchCount++
                if (config.partition) {
                    insertRowsToChildTables(batch, header)
                } else {
                    insert.executeBatch(batch.toArray() as Map[])
                }
                if (config.flushSize > 0 && batchCount % config.flushSize == 0) {
                    tx.flush()
                }
                batch.clear()
            }
        }
    }

    void load(String rootPath) {
        log.info "Determining instance num ..."
        int baseInstanceNum = config.baseOnMaxInstanceNum ? baseInstanceNum : 0
        log.info "Using ${baseInstanceNum} as lowest instance num."
        File observationsFile = new File(rootPath, TABLE.fileName)

        // Count number of rows
        log.info "Counting number of rows ..."
        int rowCount = 0
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            while (tsvReader.readNext() != null) {
                rowCount++
            }
        }
        log.info "${NumberFormat.instance.format(rowCount > 0 ? rowCount - 1 : 0)} rows in ${TABLE.fileName}."

        Writer writer
        CSVWriter tsvWriter
        if (config.write) {
            writer = new PrintWriter(config.outputFile)
            tsvWriter = Util.tsvWriter(writer)
        } else if (!config.partition && config.unlogged) {
            setLoggedMode(TABLE, false)
        }

        // Transform data: replace patient index with patient num,
        // trial visit index with trial visit num and instance num index with instance num,
        // and write transformed data to database.
        def tx = database.beginTransaction()

        log.info 'Reading, transforming and writing observations data ...'
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            String[] data = tsvReader.readNext()
            if (data == null) {
                return
            }
            int i = 1
            LinkedHashMap<String, Class> header = Util.verifyHeader(TABLE.fileName, data, columns)
            def insert = database.getInserter(TABLE, header)
            def progressBar = new ProgressBar("Insert into ${TABLE}", rowCount - 1)
            progressBar.start()
            ArrayList<Map> batch = []
            data = tsvReader.readNext()
            i++
            Integer batchCount = 0
            while (data != null) {
                try {
                    if (header.size() != data.length) {
                        throw new IllegalStateException(
                                "Data row length (${data.length}) does not match number of columns (${header.size()}).")
                    }
                    def row = Util.asMap(header, data)
                    transformRow(row, baseInstanceNum)
                    progressBar.stepBy(1)
                    loadRow(tsvWriter, header, row, batchCount, batch, insert, tx)
                } catch (Throwable e) {
                    progressBar.stop()
                    log.error "Error processing row ${i} of ${TABLE.fileName}: ${e.message}"
                    throw e
                }
                data = tsvReader.readNext()
                i++
            }
            if (batch.size() > 0) {
                batchCount++
                if (config.partition) {
                    insertRowsToChildTables(batch, header)
                } else {
                    insert.executeBatch(batch.toArray() as Map[])
                }
            }
            progressBar.stop()
            log.info "${batchCount} batches of ${config.batchSize} inserted."
            void
        }

        if (config.write) {
            tsvWriter.close()
        } else {
            if (config.partition) {
                createChildTableIndexes()
                if (config.unlogged) {
                    setLoggedModeForAllChildTables()
                }
            } else if (config.unlogged) {
                setLoggedMode(TABLE, true)
            }
        }
        database.commit(tx)
        log.info "Done loading observations data."
    }

    private void insertRowsToChildTables(List<Map> rows, LinkedHashMap<String, Class> header) {
        Map<Long, List<Map>> groupedByTrialVisitNumsRows = [:]
        for (Map row : rows) {
            Long trialVisitNum = (Long) row.get('trial_visit_num')
            if (groupedByTrialVisitNumsRows.containsKey(trialVisitNum)) {
                groupedByTrialVisitNumsRows.get(trialVisitNum).add(row)
            } else {
                groupedByTrialVisitNumsRows.put(trialVisitNum, [row])
            }
        }
        for (Map.Entry<Long, List<Map>> childRows : groupedByTrialVisitNumsRows) {
            def childTable = getOrCreateChildTable(childRows.key)
            database.getInserter(childTable, header)
                    .executeBatch(childRows.value.toArray() as Map[])
        }
    }

    private Table getOrCreateChildTable(Long trialVisitNum) {
        if (partitionToTable.containsKey(trialVisitNum)) {
            return partitionToTable.get(trialVisitNum)
        }
        Table childTable = getChildTable(trialVisitNum)
        database.jdbcTemplate.execute("CREATE ${config.unlogged ? 'UNLOGGED' : ''} TABLE ${childTable}" +
                "(check (trial_visit_num = ${trialVisitNum})) INHERITS (${TABLE})")
        partitionToTable.put(trialVisitNum, childTable)
        childTable
    }

    static Table getChildTable(long trialVisitNum) {
        new Table(TABLE.schema, "${TABLE.name}_${trialVisitNum}")
    }

    private createChildTableIndexes() {
        for (Map.Entry<Long, Table> partitionToTableEntry : partitionToTable.entrySet()) {
            createTableIndexesIfNotExistForTable(partitionToTableEntry.value, partitionToTableEntry.key)
        }
    }

    private setLoggedModeForAllChildTables() {
        for (Map.Entry<Long, Table> partitionToTableEntry : partitionToTable.entrySet()) {
            setLoggedMode(partitionToTableEntry.value, true)
        }
    }

    private createTableIndexesIfNotExistForTable(Table tbl, Long partition = null) {
        def tx = database.beginTransaction()
        log.info "Creating indexes on ${tbl} ..."
        if (partition == null) {
            for (Map.Entry<String, List<String>> parentTableIndexDeclaration: PARENT_TABLE_INDEXES.entrySet()) {
                def indexName = parentTableIndexDeclaration.key
                def columns = parentTableIndexDeclaration.value
                database.jdbcTemplate.execute(composeCreateIndexSql(tbl, indexName, columns))
            }
        } else {
            for (Map.Entry<String, List<String>> childTableIndexDeclaration: CHILD_TABLE_INDEXES.entrySet()) {
                def indexName = childTableIndexDeclaration.key
                def columns = childTableIndexDeclaration.value
                database.jdbcTemplate.execute(composeCreateIndexSql(tbl, "${indexName}_${partition}", columns))
            }
        }
        // For partitions we still might need trial_visit_num as part of composite index
        // so query could get everything it needs by index only scan.
        for (Map.Entry<String, List<String>> nonModifiersTableIndexDeclaration:
                NON_MODIFIERS_TABLE_INDEXES.entrySet()) {
            String indexName = partition ? nonModifiersTableIndexDeclaration.key + '_' + partition
                    : nonModifiersTableIndexDeclaration.key
            def columns = nonModifiersTableIndexDeclaration.value
            String createIndexSqlPart = composeCreateIndexSql(tbl, indexName, columns)
            database.jdbcTemplate.execute("${createIndexSqlPart} where modifier_cd='@'")
        }
        database.commit(tx)
    }

    private static String composeCreateIndexSql(Table table, String name, Iterable<String> columns) {
        "create index if not exists ${name} on ${table} using btree (${columns.join(', ')})"
    }

    private void setLoggedMode(Table table, boolean logged) {
        log.info "Set 'logged' on ${table} to ${logged}"
        database.jdbcTemplate.execute("alter table ${table} set ${logged ? 'logged' : 'unlogged'}")
    }
}
