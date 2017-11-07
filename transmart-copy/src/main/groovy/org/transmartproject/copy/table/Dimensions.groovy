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
class Dimensions {

    static final String table = 'i2b2metadata.dimension_description'
    static final String dimensions_file = 'i2b2metadata/dimension_description.tsv'

    final Database database

    final LinkedHashMap<String, Class> columns

    final Map<String, Long> dimensionNameToId = [:]
    final List<Long> indexToDimensionId = []

    Dimensions(Database database) {
        this.database = database
        this.columns = this.database.getColumnMetadata(table)
    }

    void fetch() {
        database.sql.rows(
                "select id, name from ${table}".toString()
        ).each { Map row ->
            def name = row['name'] as String
            def id = row['id'] as long
            dimensionNameToId[name] = id
        }
        log.info "Dimensions loaded: ${dimensionNameToId.size()} entries."
        log.debug "Entries: ${dimensionNameToId.toMapString()}"
    }

    void load(String rootPath) {
        def mappingFile = new File(rootPath, dimensions_file)
        mappingFile.withReader { reader ->
            def tsvReader = Util.tsvReader(reader)
            tsvReader.eachWithIndex { String[] data, int i ->
                if (i == 0) {
                    Util.verifyHeader(dimensions_file, data, columns)
                    return
                }
                try {
                    def dimensionData = Util.asMap(columns, data)
                    def dimensionIndex = dimensionData['id'] as long
                    if (i != dimensionIndex + 1) {
                        throw new InvalidInput("The dimensions are not in order. (Found ${dimensionIndex} on line ${i}.)")
                    }
                    def dimensionName = dimensionData['name'] as String
                    def dimensionId = dimensionNameToId[dimensionName]
                    if (!dimensionId) {
                        log.info "Inserting unknown dimension: ${dimensionName}."
                        dimensionId = database.insertEntry(table, columns, 'id', dimensionData)
                        log.info "Dimension inserted [id: ${dimensionId}]."
                    }
                    indexToDimensionId.add(dimensionId)
                    log.info "Registered dimension at index ${dimensionIndex}: ${dimensionName} [${dimensionId}]."
                } catch(Exception e) {
                    log.error "Error on line ${i} of ${dimensions_file}: ${e.message}."
                    throw e
                }
            }
        }
    }

}
