/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package inc.oracle

import au.com.bytecode.opencsv.CSVParser
@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVReader
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql
import groovy.transform.Immutable

import java.sql.SQLException
import java.util.regex.Pattern

class CsvLoader {

    public static final int DEFAULT_BATCH_SIZE = 50000

    Sql sql
    String table
    String file
    List<String> columnNames
    boolean truncate
    int batchSize = DEFAULT_BATCH_SIZE
    boolean quiet = false
    char delimiter = '\t'
    String nullValue

    @Lazy MaybeLog maybeLog = new MaybeLog(quiet: quiet)
    private long skippedLines = 0
    private long insertedLines = 0

    void load() {
        if (truncate) {
            truncateTable()
        }
        sql.withTransaction {
            skippedLines = 0
            uploadTsvFileToTable(
                    file != null && file != '-' ? new FileInputStream(file) :
                                                  System.in)
        }
    }

    Long getSkippedLines() {
        skippedLines
    }

    Long getInsertedLines() {
        insertedLines
    }

    void prepareConnection() {
        if (System.getenv('NLS_DATE_FORMAT')) {
            sql.execute "ALTER SESSION SET NLS_DATE_FORMAT = '${System.getenv('NLS_DATE_FORMAT')}'".toString()
        }
        if (System.getenv('NLS_TIMESTAMP_FORMAT')) {
            sql.execute "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = '${System.getenv('NLS_TIMESTAMP_FORMAT')}'".toString()
        }
    }

    private uploadTsvFileToTable(InputStream istr) {
        CSVReader reader = new CSVReader(new InputStreamReader(istr, 'UTF-8'),
                (delimiter ?: '\t') as char,
                CSVParser.DEFAULT_QUOTE_CHARACTER,
                CSVParser.NULL_CHARACTER)

        String[] line = reader.readNext()
        if (!line) {
            return
        }

        List<String> columns = columnNames ?: []

        def columnsInfo = getColumnTypesMap(columns)

        if (columnsInfo.size() != line.size()) {
            def message = "Expected to insert into ${columnsInfo.size()} " +
                    "columns ${columnsInfo.keySet()}, but first line of TSV file " +
                    "has ${line.size()} ($line)"
            maybeLog.err message
            throw new Exception(message)
        }

        String colGroup = constructColumnExpression(columnsInfo.keySet() as List)
        String placeHolders = constructPlaceHoldersExpression(columnsInfo)
        Closure<String[]> dataFilter = constructDataFilter(columnsInfo)

        insertedLines = 0
        int colsNum = line.length
        sql.withBatch batchSize, "INSERT INTO ${table}(${colGroup}) VALUES (${placeHolders})", {
            BatchingPreparedStatementWrapper it ->
                while (line != null) {
                    insertedLines++
                    if (insertedLines % 10000 == 0) {
                        maybeLog.print '.'
                    }

                    if (line.length != colsNum) {
                        skippedLines++
                        maybeLog.warn "${insertedLines} line contains wrong number of columns " +
                                "(${line.length} but expected to be ${colsNum}): ${line}; " +
                                "continuing anyway"
                    } else {
                        if (nullValue)
                            line = line.collect { it == nullValue ? null : it }
                        it.addBatch dataFilter(line)
                    }

                    line = reader.readNext()
                }
        }
        reader.close()
        maybeLog.out " Done after $insertedLines rows"
    }

    private static String constructColumnExpression(List<String> columns) {
        columns.collect({ column ->
            if (column ==~ '".+"') {
                column
            } else {
                '"' + column.toUpperCase(Locale.ENGLISH) + '"'
            }
        }).join(', ')
    }

    private static String constructPlaceHoldersExpression(Map<String, String> columnTypeMap) {
        columnTypeMap.collect({ entry ->
            '?'
        }).join(', ')
    }

    private static Closure<String[]> constructDataFilter(Map<String, ColumnMeta> columnsInfo) {
        List booleanColumnsPositions = columnsInfo.findIndexValues { entry ->
            entry.value.type == 'NUMBER' && entry.value.size == 1
        }.collect { it as int }
        List datePositions = columnsInfo.findIndexValues { entry ->
            entry.value.type == 'DATE'
        }.collect { it as int }
        Pattern cutMillis = ~/\.\d{3,6}$/

        return { String[] line ->
            booleanColumnsPositions.each { pos ->
                switch (line[pos]) {
                    case 't':
                        line[pos] = '1'
                        break
                    case 'f':
                        line[pos] = '0'
                        break
                }
            }

            /* DATE in Oracle was mapped to TIMESTAMP in PostgreSQL. However,
              TIMESTAMP has microsecond precision in PostgreSQL and DATE has
              second precision. We need to remove the fractional second part
              from the value, otherwise Oracle will reject it. */
            datePositions.each { pos ->
                def matcher = cutMillis.matcher(line[pos])
                line[pos] = matcher.replaceFirst('')
            }

            line /* also changes the argument! */
        }
    }

    private Map<String, ColumnMeta> getColumnTypesMap(List<String> columns = []) {
        def columnTypes = [:]
        String sqlExpression = "SELECT ${columns ? constructColumnExpression(columns) : '*'} FROM ${table} " +
                "WHERE rownum < 1"
        sql.rows sqlExpression, 1, 0, { meta ->
            columnTypes = (1..meta.columnCount).collectEntries {
                [(meta.getColumnLabel(it)): new ColumnMeta(
                        type: meta.getColumnTypeName(it),
                        size: meta.getPrecision(it))]
            }
        }

        if (columns) {
            columns.collectEntries{
                def columnName = it.toUpperCase(Locale.ENGLISH)
                [(columnName): columnTypes[columnName]]
            }
        } else {
            columnTypes
        }
    }

    private void truncateTable() {
        try {
            print "Truncating table $table... "
            sql.execute("TRUNCATE TABLE $table" as String)
            println 'Done'
        } catch (SQLException sqlException) {
            if (sqlException.errorCode == 2266) {
                // cannot truncate due to foreign keys
                maybeLog.warn 'Failed. Trying delete...'
                try {
                    sql.execute("DELETE FROM $table" as String)
                    maybeLog.out 'Done'
                } catch (SQLException e) {
                    maybeLog.err 'Failed again! Aborting'
                    throw e
                }
            } else {
                // failed for another reason
                maybeLog.err 'Failed! Aborting'
                throw sqlException
            }
        }
    }
}

@Immutable
class ColumnMeta {
    String type
    Integer size
}

class MaybeLog {
    boolean quiet

    def invokeMethod(String name, args) {
        if (!quiet) {
            Log.invokeMethod(name, args)
        }
    }

    def print(Object value) {
        invokeMethod('print', [value] as Object[])
    }
}
