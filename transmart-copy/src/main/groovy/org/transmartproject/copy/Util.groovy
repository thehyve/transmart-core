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
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Collection of utility functions for transmart-copy.
 */
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
     * Verify that the column names in {@code header} are a subset of the column names in {@code columns} and
     * at least contain all non-nullable columns.
     * Returns a map from column name to type, where the columns are ordered as in {@code header}.
     *
     * @param filename The filename to check the header for.
     * @param header The column names in the header.
     * @param columns The column names and types as defined in the database table.
     * @return a map from column name to type, where the columns are ordered as in {@code header}.
     */
    static LinkedHashMap<String, Class> verifyHeader(
            String filename, String[] header, LinkedHashMap<String, ColumnMetadata> columns) {
        if (!columns.keySet().containsAll(header)) {
            log.error "Supported columns: ${columns.keySet()}"
            log.error "Columns in file: ${header.toList()}"
            throw new IllegalStateException("Incorrect headers in file ${filename}.")
        }
        List<String> nonNullableColumns = columns.entrySet().stream()
                .filter({ Map.Entry<String, ColumnMetadata> entry -> !(entry.value.nullable) })
                .map({ Map.Entry<String, ColumnMetadata> entry -> entry.key })
                .collect(Collectors.toList())
        if (!(header as Set).containsAll(nonNullableColumns)) {
            log.error "Non-nullable columns: ${nonNullableColumns}"
            log.error "Columns in file: ${header.toList()}"
            throw new IllegalStateException("Incorrect headers in file ${filename}.")
        }
        def result = [:] as LinkedHashMap<String, Class>
        for (String column: header) {
            result[column] = columns[column].type
        }
        result
    }

    static final <T> T parseIfNotEmpty(String value, Function<String, T> parser) {
        if (value == null || value.trim().empty) {
            null
        } else {
            parser.apply(value)
        }
    }

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    static final ZoneId UTC = ZoneId.of('UTC')

    static final Timestamp parseDate(String value) {
        LocalDateTime localDateTime = LocalDateTime.from(DATE_FORMAT.parse(value))
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
            throw new IllegalArgumentException(
                    "Data row length (${data.length}) does not match number of columns (${columns.size()}).")
        }
        def result = [:] as Map<String, Object>
        columns.eachWithIndex{ String columnName, Class columnType, int i ->
            switch(columnType) {
                case String:
                    result[columnName] = parseIfNotEmpty(data[i], { it })
                    break
                case Integer:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Integer.parseInt(value) })
                    break
                case Long:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Long.parseLong(value) })
                    break
                case Double:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> Double.parseDouble(value) })
                    break
                case BigDecimal:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> new BigDecimal(value) })
                    break
                case Instant:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> parseDate(value) })
                    break
                case Boolean:
                    result[columnName] = parseIfNotEmpty(data[i], { String value -> parseBoolean(value) })
                    break
                default:
                    throw new IllegalArgumentException("Unexpected type ${columnType}")
            }
        }
        result
    }

}
