/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import me.tongfei.progressbar.ProgressBar
import org.springframework.jdbc.core.RowCallbackHandler
import org.transmartproject.copy.Copy
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@CompileStatic
class Relations {

    static final Table relation_type_table = new Table('i2b2demodata', 'relation_type')
    static final Table relation_table = new Table('i2b2demodata', 'relation')

    final Database database
    final Patients patients
    final Copy.Config config

    final LinkedHashMap<String, Class> relation_type_columns
    final LinkedHashMap<String, Class> relation_columns
    final int relationTypeIdIndex
    final int leftSubjectIdIndex
    final int rightSubjectIdIndex

    final Map<String, Long> relationTypeLabelToId = [:]
    final List<Long> indexToRelationTypeId = []

    final boolean tablesExists

    Relations(Database database, Patients patients, Copy.Config config) {
        this.database = database
        this.patients = patients
        this.config = config
        if (!database.tableExists(relation_type_table)) {
            log.warn "Table ${relation_type_table} does not exist. Skip loading of relations."
            tablesExists = false
        } else if (!database.tableExists(relation_table)) {
            log.warn "Table ${relation_table} does not exist. Skip loading of relations."
            tablesExists = false
        } else {
            tablesExists = true
        }
        if (tablesExists) {
            this.relation_type_columns = this.database.getColumnMetadata(relation_type_table)
            this.relation_columns = this.database.getColumnMetadata(relation_table)
            this.relationTypeIdIndex = relation_columns.collect { it.key }.indexOf('relation_type_id')
            this.leftSubjectIdIndex = relation_columns.collect { it.key }.indexOf('left_subject_id')
            this.rightSubjectIdIndex = relation_columns.collect { it.key }.indexOf('right_subject_id')
        } else {
            this.relation_type_columns = [:]
            this.relation_columns = [:]
            this.relationTypeIdIndex = -1
            this.leftSubjectIdIndex = -1
            this.rightSubjectIdIndex = -1
        }
    }

    @CompileStatic
    static class RelationTypeRowHandler implements RowCallbackHandler {
        final Map<String, Long> relationTypeLabelToId = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def label = rs.getString('label')
            def id = rs.getLong('id')
            relationTypeLabelToId[label] = id
        }
    }

    void fetch() {
        if (!tablesExists) {
            log.debug "Skip fetching relation types. No relation tables available."
            return
        }
        def relationTypeHandler = new RelationTypeRowHandler()
        database.jdbcTemplate.query(
                "select id, label from ${relation_type_table}".toString(),
                relationTypeHandler
        )
        relationTypeLabelToId.putAll(relationTypeHandler.relationTypeLabelToId)
        log.info "Relation types in the database: ${relationTypeLabelToId.size()} entries."
        log.debug "Entries: ${relationTypeLabelToId.toMapString()}"
    }

    void transformRow(Map<String, Object> row) {
        // replace patient index with patient num
        def relationTypeIndex = row.relation_type_id as int
        if (relationTypeIndex >= indexToRelationTypeId.size()) {
            throw new IllegalStateException("Invalid relation type index (${relationTypeIndex}). Only ${indexToRelationTypeId.size()} relation types found.")
        }
        def leftSubjectIndex = row.left_subject_id as int
        if (leftSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException("Invalid patient index (${leftSubjectIndex}). Only ${patients.indexToPatientNum.size()} patients found.")
        }
        def rightSubjectIndex = row.right_subject_id as int
        if (rightSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException("Invalid patient index (${rightSubjectIndex}). Only ${patients.indexToPatientNum.size()} patients found.")
        }
        row.relation_type_id = indexToRelationTypeId[relationTypeIndex]
        row.left_subject_id = patients.indexToPatientNum[leftSubjectIndex]
        row.right_subject_id = patients.indexToPatientNum[rightSubjectIndex]
    }

    void load(String rootPath) {
        if (!tablesExists) {
            log.debug "Skip loading of relation types and relations. No relation tables available."
            return
        }
        def tx = database.beginTransaction()
        def relationTypesFile = new File(rootPath, relation_type_table.fileName)
        // Insert relation types
        relationTypesFile.withReader { reader ->
            LinkedHashMap<String, Class> header = relation_type_columns
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(relation_type_table.fileName, data, relation_type_columns)
                    return
                }
                try {
                    def relationTypeData = Util.asMap(header, data)
                    def relationTypeIndex = relationTypeData['id'] as long
                    if (i != relationTypeIndex + 1) {
                        throw new IllegalStateException("The relation types are not in order. (Found ${relationTypeIndex} on line ${i}.)")
                    }

                    def label = relationTypeData['label'] as String
                    def id = relationTypeLabelToId[label]
                    if (id) {
                        log.info "Found relation type ${label}."
                    } else {
                        log.info "Inserting relation type ${label} ..."
                        id = database.insertEntry(relation_type_table, header, 'id', relationTypeData)
                        relationTypeLabelToId[label] = id
                    }
                    indexToRelationTypeId.add(id)
                } catch (Exception e) {
                    log.error "Error on line ${i} of ${relation_type_table.fileName}: ${e.message}."
                    throw e
                }
            }
        }
        database.commit(tx)

        def relationsFile = new File(rootPath, relation_table.fileName)
        if (!relationsFile.exists()) {
            log.info "Skip loading of relations. No file ${relation_table.fileName} found."
            return
        }

        tx = database.beginTransaction()
        // Remove relations
        log.info "Deleting relations ..."
        int relationCount = database.jdbcTemplate.update("truncate ${relation_table}".toString())
        log.info "${relationCount} relations deleted."

        // Count number of rows
        int rowCount = 0
        relationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            while (tsvReader.readNext() != null) {
                rowCount++
            }
        }

        // Transform data: replace patient index with patient num, relation type index with relation type id,
        // and write transformed data to temporary file.
        log.info 'Reading, transforming and writing relations data ...'
        relationsFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            String[] data = tsvReader.readNext()
            if (data != null) {
                int i = 1
                LinkedHashMap<String, Class> header = Util.verifyHeader(relation_table.fileName, data, relation_columns)
                def insert = database.getInserter(relation_table, header)
                final progressBar = new ProgressBar("Insert into ${relation_table}", rowCount - 1)
                progressBar.start()
                ArrayList<Map> batch = []
                data = tsvReader.readNext()
                i++
                while (data != null) {
                    try {
                        if (header.size() != data.length) {
                            throw new IllegalStateException("Data row length (${data.length}) does not match number of columns (${header.size()}).")
                        }
                        def row = Util.asMap(header, data)
                        transformRow(row)
                        progressBar.stepBy(1)
                        batch.add(row)
                        if (batch.size() == config.batchSize) {
                            insert.executeBatch(batch.toArray() as Map[])
                            batch = []
                        }
                    } catch (Exception e) {
                        progressBar.stop()
                        log.error "Error processing row ${i} of ${relation_table.fileName}", e
                        throw e
                    }
                    data = tsvReader.readNext()
                    i++
                }
                if (batch.size() > 0) {
                    insert.executeBatch(batch.toArray() as Map[])
                }
                progressBar.stop()
                return
            }
        }
        database.commit(tx)
        log.info 'Done loading relations data.'
    }

}
