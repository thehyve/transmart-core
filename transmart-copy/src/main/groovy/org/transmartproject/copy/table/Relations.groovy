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
import org.transmartproject.copy.ColumnMetadata
import org.transmartproject.copy.Copy
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Fetching and loading of relation types and relations.
 */
@Slf4j
@CompileStatic
class Relations {

    static final Table RELATION_TYPE_TABLE = new Table('i2b2demodata', 'relation_type')
    static final Table RELATION_TABLE = new Table('i2b2demodata', 'relation')

    final Database database
    final Patients patients
    final Copy.Config config

    final LinkedHashMap<String, ColumnMetadata> relationTypeColumns
    final LinkedHashMap<String, ColumnMetadata> relationColumns
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
        if (!database.tableExists(RELATION_TYPE_TABLE)) {
            log.warn "Table ${RELATION_TYPE_TABLE} does not exist. Skip loading of relations."
            tablesExists = false
        } else if (!database.tableExists(RELATION_TABLE)) {
            log.warn "Table ${RELATION_TABLE} does not exist. Skip loading of relations."
            tablesExists = false
        } else {
            tablesExists = true
        }
        if (tablesExists) {
            this.relationTypeColumns = this.database.getColumnMetadata(RELATION_TYPE_TABLE)
            this.relationColumns = this.database.getColumnMetadata(RELATION_TABLE)
            this.relationTypeIdIndex = relationColumns.collect { it.key }.indexOf('relation_type_id')
            this.leftSubjectIdIndex = relationColumns.collect { it.key }.indexOf('left_subject_id')
            this.rightSubjectIdIndex = relationColumns.collect { it.key }.indexOf('right_subject_id')
        } else {
            this.relationTypeColumns = [:]
            this.relationColumns = [:]
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
                "select id, label from ${RELATION_TYPE_TABLE}".toString(),
                relationTypeHandler
        )
        relationTypeLabelToId.putAll(relationTypeHandler.relationTypeLabelToId)
        log.info "Relation types in the database: ${relationTypeLabelToId.size()} entries."
        log.debug "Entries: ${relationTypeLabelToId.toMapString()}"
    }

    void transformRow(Map<String, Object> row) {
        // replace patient index with patient num
        int relationTypeIndex = ((BigDecimal) row.relation_type_id).intValueExact()
        if (relationTypeIndex >= indexToRelationTypeId.size()) {
            throw new IllegalStateException(
                    "Invalid relation type index (${relationTypeIndex}). " +
                            "Only ${indexToRelationTypeId.size()} relation types found.")
        }
        int leftSubjectIndex = ((BigDecimal) row.left_subject_id).intValueExact()
        if (leftSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException(
                    "Invalid patient index (${leftSubjectIndex}). " +
                            "Only ${patients.indexToPatientNum.size()} patients found.")
        }
        int rightSubjectIndex = ((BigDecimal) row.right_subject_id).intValueExact()
        if (rightSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new IllegalStateException(
                    "Invalid patient index (${rightSubjectIndex}). " +
                            "Only ${patients.indexToPatientNum.size()} patients found.")
        }
        row.relation_type_id = indexToRelationTypeId[relationTypeIndex]
        row.left_subject_id = patients.indexToPatientNum[leftSubjectIndex]
        row.right_subject_id = patients.indexToPatientNum[rightSubjectIndex]
    }

    private void loadRelationTypes(String rootPath) {
        def relationTypesFile = new File(rootPath, RELATION_TYPE_TABLE.fileName)
        // Insert relation types
        relationTypesFile.withReader { reader ->
            LinkedHashMap<String, Class> header
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(RELATION_TYPE_TABLE.fileName, data, relationTypeColumns)
                    return
                }
                try {
                    def relationTypeData = Util.asMap(header, data)
                    def relationTypeIndex = relationTypeData['id'] as long
                    if (i != relationTypeIndex + 1) {
                        throw new IllegalStateException(
                                "The relation types are not in order. " +
                                        "(Found ${relationTypeIndex} on line ${i}.)")
                    }

                    def label = relationTypeData['label'] as String
                    def id = relationTypeLabelToId[label]
                    if (id) {
                        log.info "Found relation type ${label}."
                    } else {
                        log.info "Inserting relation type ${label} ..."
                        id = database.insertEntry(RELATION_TYPE_TABLE, header, 'id', relationTypeData)
                        relationTypeLabelToId[label] = id
                    }
                    indexToRelationTypeId.add(id)
                } catch (Throwable e) {
                    log.error "Error on line ${i} of ${RELATION_TYPE_TABLE.fileName}: ${e.message}."
                    throw e
                }
            }
        }
    }

    private void loadRelations(String rootPath) {
        def relationsFile = new File(rootPath, RELATION_TABLE.fileName)
        if (!relationsFile.exists()) {
            log.info "Skip loading of relations. No file ${RELATION_TABLE.fileName} found."
            return
        }

        // Remove relations
        log.info "Deleting relations ..."
        int relationCount = database.jdbcTemplate.update("truncate ${RELATION_TABLE}".toString())
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
                LinkedHashMap<String, Class> header = Util.verifyHeader(RELATION_TABLE.fileName, data, relationColumns)
                def insert = database.getInserter(RELATION_TABLE, header)
                def progressBar = new ProgressBar("Insert into ${RELATION_TABLE}", rowCount - 1)
                progressBar.start()
                def batch = [] as ArrayList<Map>
                data = tsvReader.readNext()
                i++
                while (data != null) {
                    try {
                        if (header.size() != data.length) {
                            throw new IllegalStateException(
                                    "Data row length (${data.length}) does not match " +
                                            "number of columns (${header.size()}).")
                        }
                        def row = Util.asMap(header, data)
                        transformRow(row)
                        progressBar.stepBy(1)
                        batch.add(row)
                        if (batch.size() == config.batchSize) {
                            insert.executeBatch(batch.toArray() as Map[])
                            batch = []
                        }
                    } catch (Throwable e) {
                        progressBar.stop()
                        log.error "Error processing row ${i} of ${RELATION_TABLE.fileName}", e
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
    }

    void load(String rootPath) {
        if (!tablesExists) {
            log.debug "Skip loading of relation types and relations. No relation tables available."
            return
        }
        def tx = database.beginTransaction()
        loadRelationTypes(rootPath)
        database.commit(tx)
        tx = database.beginTransaction()
        loadRelations(rootPath)
        database.commit(tx)
        log.info 'Done loading relations data.'
    }

}
