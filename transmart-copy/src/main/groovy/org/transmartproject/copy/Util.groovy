/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import com.opencsv.ICSVParser
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Slf4j
@CompileStatic
class Util {

    static CSVReader tsvReader(Reader reader) {
        new CSVReaderBuilder(reader)
                .withCSVParser(
                    new CSVParserBuilder()
                        .withSeparator('\t' as char)
                        .withEscapeChar(ICSVParser.NULL_CHARACTER)
                        .build())
                .build()
    }

    static CSVWriter tsvWriter(Writer writer) {
        new CSVWriter(writer, '\t' as char, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER)
    }

    /**
     * Verify that the column names in {@code header} are the same as the column names in {@code columns}.
     * Returns a map from column name to type, where the columns are ordered as in {@code header}.
     *
     * @param filename The filename to check the header for.
     * @param header The column names in the header.
     * @param columns The column names and types as defined in the database table.
     * @return a map from column name to type, where the columns are ordered as in {@code header}.
     */
    static LinkedHashMap<String, Class> verifyHeader(String filename, String[] header, LinkedHashMap<String, Class> columns) {
        List<String> columnNames = columns.collect { it.key }
        if ((header as Set) != (columnNames as Set)) {
            log.error "Expected: ${columnNames}"
            log.error "Was: ${header.toList()}"
            throw new IllegalStateException("Incorrect headers in file ${filename}.")
        }
        LinkedHashMap result = [:]
        header.each { String column ->
            result[column] = columns[column]
        }
        result
    }

    static final <T> T parseIfNotEmpty(String value, Closure<T> parser) {
        if (value == null || value.trim().empty) {
            null
        } else {
            parser(value)
        }
    }

    static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    static final ZoneId UTC = ZoneId.of('UTC')

    static final Timestamp parseDate(String value) {
        LocalDateTime localDateTime = LocalDateTime.from(dateFormat.parse(value))
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, UTC)
        def result = Timestamp.from(Instant.from(zonedDateTime))
        log.debug "${value} -> ${localDateTime} -> ${zonedDateTime} -> ${result}"
        result
    }

    static final Boolean parseBoolean(String value) {
        if (!value) {
            return null
        }
        String lValue = value.toLowerCase()
        if (lValue in ['t', 'f']) {
           return lValue == 't'
        }
        Boolean.parseBoolean(value)
    }

    static final Map<String, Object> asMap(final LinkedHashMap<String, Class> columns, final String[] data) {
        if (columns.size() != data.length) {
            throw new IllegalArgumentException("Data row length (${data.length}) does not match number of columns (${columns.size()}).")
        }
        def result = [:] as Map<String, Object>
        columns.eachWithIndex{ String columnName, Class columnType, int i ->
            switch(columnType) {
                case String.class:
                    result[columnName] = parseIfNotEmpty(data[i], { it })
                    break
                case Integer.class:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Integer.parseInt(value) })
                    break
                case Long.class:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Long.parseLong(value) })
                    break
                case Double.class:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Double.parseDouble(value) })
                    break
                case Instant.class:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> parseDate(value) })
                    break
                case Boolean.class:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> parseBoolean(value) })
                    break
                default:
                    throw new IllegalArgumentException("Unexpected type ${columnType}")
            }
        }
        result
    }

}
