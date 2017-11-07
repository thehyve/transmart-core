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
class Relations {

    static final String relation_type_table = 'i2b2demodata.relation_type'
    static final String relation_type_file = 'i2b2demodata/relation_type.tsv'
    static final String relation_table = 'i2b2demodata.relation'
    static final String relation_file = 'i2b2demodata/relation.tsv'

    final Database database

    final Patients patients

    final LinkedHashMap<String, Class> relation_type_columns
    final LinkedHashMap<String, Class> relation_columns
    final int relationTypeIdIndex
    final int leftSubjectIdIndex
    final int rightSubjectIdIndex

    final Map<String, Long> relationTypeLabelToId = [:]
    final List<Long> indexToRelationTypeId = []

    final boolean tablesExists

    Relations(Database database, Patients patients) {
        this.database = database
        this.patients = patients
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

    void fetch() {
        if (!tablesExists) {
            log.debug "Skip fetching relation types. No relation tables available."
            return
        }
        database.sql.rows(
                "select id, label from ${relation_type_table}".toString()
        ).each { Map row ->
            def label = row['label'] as String
            def id = row['id'] as long
            relationTypeLabelToId[label] = id
        }
        log.info "Relation types loaded: ${relationTypeLabelToId.size()} entries."
        log.debug "Entries: ${relationTypeLabelToId.toMapString()}"
    }

    String[] transformRow(final String[] data) {
        String[] result = Arrays.copyOf(data, data.length)
        // replace patient index with patient num
        def relationTypeIndex = data[relationTypeIdIndex] as int
        if (relationTypeIndex >= indexToRelationTypeId.size()) {
            throw new InvalidInput("Invalid relation type index (${relationTypeIndex}). Only ${indexToRelationTypeId.size()} relation types found.")
        }
        def leftSubjectIndex = data[leftSubjectIdIndex] as int
        if (leftSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new InvalidInput("Invalid patient index (${leftSubjectIndex}). Only ${patients.indexToPatientNum.size()} patients found.")
        }
        def rightSubjectIndex = data[rightSubjectIdIndex] as int
        if (rightSubjectIndex >= patients.indexToPatientNum.size()) {
            throw new InvalidInput("Invalid patient index (${rightSubjectIndex}). Only ${patients.indexToPatientNum.size()} patients found.")
        }
        result[relationTypeIdIndex] = indexToRelationTypeId[relationTypeIndex]
        result[leftSubjectIdIndex] = patients.indexToPatientNum[leftSubjectIndex]
        result[rightSubjectIdIndex] = patients.indexToPatientNum[rightSubjectIndex]
        result
    }

    void load(String rootPath, File tmpDir) {
        if (!tablesExists) {
            log.debug "Skip loading of relation types and relations. No relation tables available."
            return
        }
        database.sql.withTransaction {
            // Insert relation types
            def relationTypesFile = new File(rootPath, relation_type_file)
            relationTypesFile.withReader { reader ->
                def tsvReader = Util.tsvReader(reader)
                tsvReader.eachWithIndex { String[] data, int i ->
                    if (i == 0) {
                        Util.verifyHeader(relation_type_file, data, relation_type_columns)
                        return
                    }
                    try {
                        def relationTypeData = Util.asMap(relation_type_columns, data)
                        def relationTypeIndex = relationTypeData['id'] as long
                        if (i != relationTypeIndex + 1) {
                            throw new InvalidInput("The relation types are not in order. (Found ${relationTypeIndex} on line ${i}.)")
                        }

                        def label = relationTypeData['label'] as String
                        def id = relationTypeLabelToId[label]
                        if (id) {
                            log.info "Found relation type ${label}."
                        } else {
                            log.info "Inserting relation type ${label} ..."
                            id = database.insertEntry(relation_type_table, relation_type_columns, 'id', relationTypeData)
                            relationTypeLabelToId[label] = id
                        }
                        indexToRelationTypeId.add(id)
                    } catch (Exception e) {
                        log.error "Error on line ${i} of ${relation_type_file}: ${e.message}."
                        throw e
                    }
                }
            }

            // Remove relations
            log.info "Deleting relations ..."
            int relationCount = database.sql.executeUpdate("truncate ${relation_table}".toString())
            log.info "${relationCount} relations deleted."

            // Insert relations
            def relationsFile = new File(rootPath, relation_file)
            File tempFile = File.createTempFile('relations_', '.tsv', tmpDir)
            // Transform data: replace patient index with patient num, relation type index with relation type id,
            // and write transformed data to temporary file.
            log.info 'Reading and transforming relations data ...'
            log.info "Writing to temporary file ${tempFile.path} ..."
            int n = 0
            relationsFile.withReader { reader ->
                tempFile.withPrintWriter { writer ->
                    def tsvReader = Util.tsvReader(reader)
                    def tsvWriter = Util.tsvWriter(writer)
                    tsvReader.eachWithIndex { String[] data, int i ->
                        if (i == 0) {
                            Util.verifyHeader(tempFile.path, data, relation_columns)
                            return
                        }
                        n = i
                        try {
                            if (relation_columns.size() != data.length) {
                                throw new InvalidInput("Data row length (${data.length}) does not match number of columns (${relation_columns.size()}).")
                            }
                            String[] result = transformRow(data)
                            tsvWriter.writeNext(result)
                            if (i % 1000 == 0) {
                                log.info "${i} rows read ..."
                                tsvWriter.flush()
                            }
                        } catch (Exception e) {
                            log.error "Error processing row ${i} of ${relation_file}", e
                        }
                    }
                    tsvWriter.flush()
                }
            }
            log.info "Done reading and transforming relations data. ${n} rows read."
            // Insert transformed data
            log.info 'Loading relations data into the database ...'
            database.copyFile(relation_table, tempFile)
            log.info 'Done loading observations data.'
        }
    }

}
