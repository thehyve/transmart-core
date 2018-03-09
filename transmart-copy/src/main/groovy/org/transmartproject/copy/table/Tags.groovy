/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy.table

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.RowCallbackHandler
import org.transmartproject.copy.Database
import org.transmartproject.copy.Table
import org.transmartproject.copy.Util

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@CompileStatic
class Tags {

    @CompileStatic
    @Immutable
    static class TagKey {
        String path
        String tagType

        @Override
        String toString() {
            "(${path}, ${tagType})"
        }
    }

    static final Table table = new Table('i2b2metadata', 'i2b2_tags')

    final Database database

    final LinkedHashMap<String, Class> columns

    final TreeNodes treeNodes

    final Map<TagKey, Long> tags = [:]
    final Map<String, Integer> maxIndexes = [:]


    Tags(Database database, TreeNodes treeNodes) {
        this.database = database
        this.treeNodes = treeNodes
        this.columns = this.database.getColumnMetadata(table)
    }

    @CompileStatic
    static class TagRowHandler implements RowCallbackHandler {
        final Map<TagKey, Long> tags = [:]
        final Map<String, Integer> maxIndexes = [:]

        @Override
        void processRow(ResultSet rs) throws SQLException {
            def id = rs.getLong('tag_id')
            def path = rs.getString('path')
            def tagType = rs.getString('tag_type')
            def index = rs.getInt('tags_idx')
            def key = new TagKey(path: path, tagType: tagType)
            tags.put(key, id)
            def maxIndex = maxIndexes[path]
            if (maxIndex == null || index > maxIndex) {
                maxIndexes[path] = index
            }
        }
    }

    void fetch() {
        def tagHandler = new TagRowHandler()
        database.jdbcTemplate.query(
                "select tag_id, path, tag_type, tags_idx from ${table}".toString(),
                tagHandler
        )
        tags.putAll(tagHandler.tags)
        maxIndexes.putAll(tagHandler.maxIndexes)
        log.info "Tags in the database: ${tags.size()}."
    }

    void load(String rootPath) {
        def tagsFile = new File(rootPath, table.fileName)
        if (!tagsFile.exists()) {
            log.info "Skip loading of tags. No file ${table.fileName} found."
            return
        }
        def tx = database.beginTransaction()
        tagsFile.withReader { reader ->
            log.info "Reading tags from file ..."
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
                    def tagData = Util.asMap(header, data)
                    def path = tagData['path'] as String
                    if (!(path in treeNodes.pathsFromFile)) {
                        throw new IllegalStateException("Tag found for tree path ${path} that does not exist in ${TreeNodes.table.fileName}.")
                    } else {
                        def tagType = tagData['tag_type'] as String
                        def index = tagData['tags_idx'] as int
                        def key = new TagKey(path: path, tagType: tagType)
                        if (tags.containsKey(key)) {
                            existingCount++
                            log.debug "Tag ${key} already present."
                        } else {
                            // increase tag index with the number of existing tags for the node
                            tagData['tags_idx'] = (maxIndexes[path] ?: 0) + index + 1
                            insertCount++
                            log.debug "Inserting new tag ${key} ..."
                            Long id = database.insertEntry(table, header, 'tag_id', tagData)
                            tags.put(key, id)
                        }
                    }
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${table.fileName}: ${e.message}."
                    throw e
                }
            }
            database.commit(tx)
            log.info "${existingCount} existing tags found."
            log.info "${insertCount} tags inserted."
        }
    }

}
