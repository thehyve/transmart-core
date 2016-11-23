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

@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import DatabaseConnection
import au.com.bytecode.opencsv.CSVWriter
import groovy.sql.GroovyResultSet
import groovy.sql.Sql

def parseOptions() {
    def cli = new CliBuilder(usage: "DumpTableData.groovy")
    cli.t 'qualified table name', required: true, longOpt: 'table', args: 1, argName: 'table'
    cli.o 'tsv file; stdout if unspecified', longOpt: 'file', args: 1, argName: 'file'
    cli.c 'column names', longOpt: 'cols', argName: 'col1,col2,...', args: 1
    cli.h 'include column names in result file', longOpt: 'header'
    def options = cli.parse(args)
    options
}

private String constructColumnExpression(Map<String, String> columnTypeMap) {
    columnTypeMap.collect({ entry ->
        if (entry.key ==~ '".+"') {
            entry.key
        } else {
            '"' + entry.key.toUpperCase(Locale.ENGLISH) + '"'
        }
    }).join(', ')
}

def dumpTableDataToTsvFile(Sql sql, OutputStream ostr, String table, String csColumns, boolean dumpHeader) {
    List<String> columns = csColumns ? csColumns.split(',') as List : []
    CSVWriter writer = new CSVWriter(new OutputStreamWriter(ostr, 'UTF-8'), '\t' as char)
    try {
        sql.eachRow "SELECT ${columns ? constructColumnExpression(columns) : '*'} FROM ${table}".toString(), { row ->
            if (dumpHeader && row.getRow() < 2) {
                writer.writeNext(getTitlesLine(row))
            }
            writer.writeNext(getDataLine(row))
        }
    } finally {
        writer.close()
    }
}

private String[] getTitlesLine(GroovyResultSet rs) {
    def meta = rs.getMetaData()
    (1..meta.columnCount).collect { pos ->
        meta.getColumnName(pos)
    }.toArray()
}

private String[] getDataLine(GroovyResultSet rs) {
    def meta = rs.getMetaData()
    (1..meta.columnCount).collect { pos ->
        convertToString(rs.getAt(pos - 1), meta.getColumnTypeName(pos))
    }.toArray()
}

private String convertToString(Object value, String sqlDataType) {
    value == null ? null : value.toString()
}

options = parseOptions()

if (!options) {
    System.exit 1
}

def sql = DatabaseConnection.setupDatabaseConnection()
if(System.getenv('NLS_DATE_FORMAT')) {
        sql.execute "ALTER SESSION SET NLS_DATE_FORMAT = '${System.getenv('NLS_DATE_FORMAT')}'".toString()
}
if(System.getenv('NLS_TIMESTAMP_FORMAT')) {
        sql.execute "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = '${System.getenv('NLS_TIMESTAMP_FORMAT')}'".toString()
}
sql.withTransaction {
    dumpTableDataToTsvFile(sql,
            options.file ? new FileOutputStream(options.file) : System.out,
            options.table,
            options.c ?: '',
            options.h)
}

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
