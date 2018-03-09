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
class TreeNodes {

    static final Table table = new Table('i2b2metadata', 'i2b2_secure')

    final Database database

    final LinkedHashMap<String, Class> columns

    final Studies studies
    final Concepts concepts

    final Set<String> paths = []
    final Set<String> pathsFromFile = []

    TreeNodes(Database database, Studies studies, Concepts concepts) {
        this.database = database
        this.studies = studies
        this.concepts = concepts
        this.columns = this.database.getColumnMetadata(table)
    }

    @CompileStatic
    static class TreeNodeRowHandler implements RowCallbackHandler {
        final List<String> paths = []

        @Override
        void processRow(ResultSet rs) throws SQLException {
            paths.add(rs.getString('c_fullname'))
        }
    }

    void fetch() {
        def treeNodeHandler = new TreeNodeRowHandler()
        database.jdbcTemplate.query(
                "select c_fullname from ${table}".toString(),
                treeNodeHandler
        )
        paths.addAll(treeNodeHandler.paths)
        log.info "Tree nodes in the database: ${paths.size()}."
        log.debug "Paths: ${paths}"
    }

    static final Set<String> operators = ['=', 'like'] as Set

    void validateNode(Map<String, Object> treeNode) {
        def path = (treeNode['c_fullname'] as String)?.trim()
        if (!path.contains('\\')) {
            throw new IllegalStateException("Invalid path: ${path}")
        }
        def visualAttributes = treeNode['c_visualattributes'] as String
        def tableName = (treeNode['c_tablename'] as String)?.trim()?.toLowerCase()
        def columnName = (treeNode['c_columnname'] as String)?.trim()?.toLowerCase()
        def operator = (treeNode['c_operator'] as String)?.trim()?.toLowerCase()
        def dimCode = treeNode['c_dimcode'] as String
        if (visualAttributes?.length() > 3) {
            throw new IllegalStateException("Invalid visual attributes for ${path}: '${visualAttributes}' (maximum length is 3).")
        }
        if (visualAttributes?.startsWith('C')) {
            log.debug "Container: ${path}"
            return
        }

        def isLeafNode = visualAttributes?.startsWith('L')
        def isStudyNode = visualAttributes?.length() == 3 && visualAttributes[2] == 'S'
        if (tableName == 'concept_dimension') {
            if (!(operator in operators)) {
                throw new IllegalStateException("Unexpected operator for node ${path}: ${operator}.")
            }
            if (columnName == 'concept_cd') {
                if (!(dimCode in concepts.conceptCodes)) {
                    throw new IllegalStateException("Unknown concept code for node ${path}: ${dimCode}.")
                }
            } else if (columnName == 'concept_path') {
                if (!(dimCode in concepts.conceptPaths) && isLeafNode) {
                    throw new IllegalStateException("Unknown concept path for node ${path}: ${dimCode}.")
                }
            } else {
                throw new IllegalStateException("Unexpected column name for concept node ${path}: ${columnName}.")
            }
        } else if (isStudyNode && tableName == 'study') {
            if (!(operator in operators)) {
                throw new IllegalStateException("Unexpected operator for node ${path}: ${operator}.")
            }
            if (columnName == 'study_id') {
                if (!(dimCode in studies.studyIdToStudyNum.keySet())) {
                    throw new IllegalStateException("Unknown study id for node ${path}: ${dimCode}.")
                }
            } else {
                throw new IllegalStateException("Unexpected column name for study node ${path}: ${columnName}.")
            }
        } else if (isLeafNode || isStudyNode) {
            throw new IllegalStateException("Unexpected table name for node ${path}: ${tableName}.")
        }
    }

    void load(String rootPath) {
        def treeNodesFile = new File(rootPath, table.fileName)
        def tx = database.beginTransaction()
        updateTreeNodePaths(concepts.oldToNewConceptPath)
        treeNodesFile.withReader { reader ->
            log.info "Reading tree nodes from file ..."
            def insertCount = 0
            def existingCount = 0
            LinkedHashMap<String, Class> header = columns
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    header = Util.verifyHeader(table.fileName, data, columns)
                    return
                }
                try {
                    def treeNodeData = Util.asMap(header, data)
                    validateNode(treeNodeData)
                    def path = treeNodeData['c_fullname'] as String
                    pathsFromFile.add(path)
                    if (path in paths) {
                        existingCount++
                        log.debug "Found existing tree node path: ${path}."
                    } else {
                        insertCount++
                        log.debug "Inserting new tree node: ${path} ..."
                        database.insertEntry(table, header, treeNodeData)
                        paths.add(path)
                    }
                } catch (Exception e) {
                    log.error "Error on line ${i} of ${table.fileName}: ${e.message}."
                    throw e
                }
            }
            database.commit(tx)
            log.info "${existingCount} existing tree nodes found."
            log.info "${insertCount} tree nodes inserted."
        }
    }

    /**
     * Workaround solution to update tree nodes when concept path has been changed
     * @param oldToNewConceptPath
     */
    private updateTreeNodePaths(Map<String, String> oldToNewConceptPath) {
        if (oldToNewConceptPath) {
            log.info("Updating ${oldToNewConceptPath.size()} path(s).")
            Map<String, ?>[] params = oldToNewConceptPath.collect { String oldPath, String newPath ->
                [newPath: newPath,
                 oldPath: oldPath]
            } as Map<String, ?>[]
            int[] removedRowsBatch = database.namedParameterJdbcTemplate.batchUpdate(
                    """delete from ${
                        table
                    } where c_fullname = :oldPath and c_dimcode = :oldPath and c_columnname ilike 'concept_path' and c_tablename ilike 'concept_dimension'""".toString(),
                    params)
            for (int i = 0; i < removedRowsBatch.length; i++) {
                log.info("${removedRowsBatch[i]} node with '${params[i].oldPath}' path was removed.")
                paths.remove(params[i].oldPath)
            }
            int[] updatedRowsBatch = database.namedParameterJdbcTemplate.batchUpdate(
                    """update ${table} set c_dimcode = :newPath
                       where c_dimcode = :oldPath and c_columnname ilike 'concept_path' and c_tablename ilike 'concept_dimension'""".toString(),
                    params)
            for (int i = 0; i < updatedRowsBatch.length; i++) {
                log.info("${updatedRowsBatch[i]} nodes with reference to '${params[i].oldPath}' concept path were updated to point to '${params[i].newPath}'.")
            }
        } else {
            log.debug("No paths to update. Exit.")
        }
    }

}
