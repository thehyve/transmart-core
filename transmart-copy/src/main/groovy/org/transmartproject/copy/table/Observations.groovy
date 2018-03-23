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

    static final Table table = new Table('i2b2demodata', 'observation_fact')

    static final Timestamp emptyDate = Timestamp.valueOf('0001-01-01 00:00:00')

    final Database database
    final Copy.Config config

    final LinkedHashMap<String, Class> columns

    final Studies studies
    final Concepts concepts
    final Patients patients

    private final Map<Integer, Table> partitionToTable = [:]

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

    void transformRow(final Map<String, Object> row, final int baseInstanceNum) {
        // replace patient index with patient num
        int patientIndex = row.get('patient_num') as int
        if (patientIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException("Patient index higher than the number of patients (${patients.indexToPatientNum.size()})")
        }
        row.put('patient_num', patients.indexToPatientNum[patientIndex])
        int trialVisitIndex = row.get('trial_visit_num') as int
        if (trialVisitIndex >= studies.indexToTrialVisitNum.size()) {
            throw new IllegalStateException("Trial visit index higher than the number of trial visits (${studies.indexToTrialVisitNum.size()})")
        }
        row.put('trial_visit_num', studies.indexToTrialVisitNum[trialVisitIndex])
        String conceptCode = (String) row.get('concept_cd')
        if (!(conceptCode in concepts.conceptCodes)) {
            throw new IllegalStateException("Unknown concept code: ${conceptCode}")
        }
        Integer instanceIndex = row.get('instance_num') as Integer
        row.put('instance_num', baseInstanceNum + instanceIndex)
        if (!row.get('start_date')) {
            row.put('start_date', emptyDate)
        }
    }

    static final Map<String, List> parentTableIndexes = [
            'idx_fact_patient_num'    : ['patient_num'],
            'idx_fact_concept'        : ['concept_cd'],
            'idx_fact_trial_visit_num': ['trial_visit_num'],
    ]

    static final Map<String, List> childTableIndexes = [
            'idx_fact_patient_num': ['patient_num'],
            'idx_fact_concept'    : ['concept_cd'],
    ]

    static final Map<String, List> nonModifiersTableIndexes = [
            'observation_fact_pct_idx': ['patient_num', 'concept_cd', 'trial_visit_num'],
    ]

    void restoreTableIndexesIfNotExist() {
        createTableIndexesIfNotExistForTable(table)
    }

    void removeObservationsForTrials(Set<Integer> trialVisitNums) {
        if (!trialVisitNums) {
            return
        }
        Set<Table> posibleCandidateTablesToDrop = trialVisitNums.collect { getChildTable(it) } as Set
        Set<Table> childTables = database.getChildTables(table)
        for (Table dropTableCandidate : posibleCandidateTablesToDrop) {
            if (dropTableCandidate in childTables) {
                database.dropTable(dropTableCandidate)
                log.info "${dropTableCandidate} child table has been dropped."
            }
        }
        int observationCount = database.namedParameterJdbcTemplate.update(
                """delete from ${table} where trial_visit_num in 
                (:trialVisitNums)""".toString(),
                [trialVisitNums: trialVisitNums]
        )
        log.info "${observationCount} observations deleted from ${table}."
    }

    void dropTableIndexesIfExist() {
        def tx = database.beginTransaction()
        log.info "Drop indexes on ${table} ..."
        (parentTableIndexes.keySet() + nonModifiersTableIndexes.keySet()).each { name ->
            database.jdbcTemplate.execute("drop index if exists i2b2demodata.${name}")
        }
        database.commit(tx)
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
        } else if (!config.partition && config.unlogged) {
            setLoggedMode(table, false)
        }

        // Transform data: replace patient index with patient num,
        // trial visit index with trial visit num and instance num index with instance num,
        // and write transformed data to database.
        def tx = database.beginTransaction()

        log.info 'Reading, transforming and writing observations data ...'
        observationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            String[] data = tsvReader.readNext()
            if (data != null) {
                int i = 1
                LinkedHashMap<String, Class> header = Util.verifyHeader(table.fileName, data, columns)
                def insert = database.getInserter(table, header)
                final progressBar = new ProgressBar("Insert into ${table}", rowCount - 1)
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
                                if (config.partition) {
                                    insertRowsToChildTables(batch, header)
                                } else {
                                    insert.executeBatch(batch.toArray() as Map[])
                                }
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
                    if (config.partition) {
                        insertRowsToChildTables(batch, header)
                    } else {
                        insert.executeBatch(batch.toArray() as Map[])
                    }
                }
                progressBar.stop()
                log.info "${batchCount} batches of ${config.batchSize} inserted."
                return
            }
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
                setLoggedMode(table, true)
            }
        }
        database.commit(tx)
        log.info "Done loading observations data."
    }

    private void insertRowsToChildTables(List<Map> rows, LinkedHashMap<String, Class> header) {
        Map<Integer, List<Map>> groupedByTrialVisitNumsRows = new HashMap<>()
        for (Map row : rows) {
            Integer trialVisitNum = (Integer) row.get('trial_visit_num')
            if (groupedByTrialVisitNumsRows.containsKey(trialVisitNum)) {
                groupedByTrialVisitNumsRows.get(trialVisitNum).add(row)
            } else {
                groupedByTrialVisitNumsRows.put(trialVisitNum, [row])
            }
        }
        for (Map.Entry<Integer, List<Map>> childRows : groupedByTrialVisitNumsRows) {
            def childTable = getOrCreateChildTable(childRows.key)
            database.getInserter(childTable, header)
                    .executeBatch(childRows.value.toArray() as Map[])
        }
    }

    private Table getOrCreateChildTable(Integer trialVisitNum) {
        if (partitionToTable.containsKey(trialVisitNum)) {
            return partitionToTable.get(trialVisitNum)
        }
        Table childTable = getChildTable(trialVisitNum)
        database.jdbcTemplate.execute("CREATE ${config.unlogged ? 'UNLOGGED' : ''} TABLE ${childTable}" +
                "(check (trial_visit_num = ${trialVisitNum})) INHERITS (${table})")
        partitionToTable.put(trialVisitNum, childTable)
        return childTable
    }

    private static Table getChildTable(int trialVisitNum) {
        new Table(table.schema, "${table.name}_${trialVisitNum}")
    }

    private createChildTableIndexes() {
        for (Map.Entry<Integer, Table> partitionToTableEntry : partitionToTable.entrySet()) {
            createTableIndexesIfNotExistForTable(partitionToTableEntry.value, partitionToTableEntry.key)
        }
    }

    private setLoggedModeForAllChildTables() {
        for (Map.Entry<Integer, Table> partitionToTableEntry : partitionToTable.entrySet()) {
            setLoggedMode(partitionToTableEntry.value, true)
        }
    }

    private createTableIndexesIfNotExistForTable(Table tbl, Integer partition = null) {
        def tx = database.beginTransaction()
        log.info "Creating indexes on ${tbl} ..."
        if (partition == null) {
            parentTableIndexes.each { name, columns ->
                database.jdbcTemplate.execute(composeCreateIndexSql(tbl, name, columns))
            }
        } else {
            childTableIndexes.each { name, columns ->
                database.jdbcTemplate.execute(composeCreateIndexSql(tbl, "${name}_${partition}", columns))
            }
        }
        //For partitions we still might need trial_visit_num as part of composite index so query could get everything it needs by index only scan.
        nonModifiersTableIndexes.each { name, columns ->
            String indexName = partition ? name + '_' + partition : name
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
