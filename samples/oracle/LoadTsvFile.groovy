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

import inc.oracle.CsvLoader
import inc.oracle.Log
import inc.oracle.SqlProducer

def parseOptions() {
    def cli = new CliBuilder(usage: "LoadTsvFile.groovy")
    cli.t 'qualified table name', required: true, longOpt: 'table', args: 1, argName: 'table'
    cli.f 'tsv file; stdin if unspecified or -', longOpt: 'file', args: 1, argName: 'file'
    cli.d 'single character used as delimiter; tab if unspecified', longOpt: 'delimiter', args: 1, argName: 'delimiter'
    cli.n 'string used as NULL value; no NULL detection if unspecified', longOpt: 'null', args: 1
    cli.c 'column names', longOpt: 'cols', argName: 'col1,col2,...', args: 1
    cli._ 'truncate table before', longOpt: 'truncate'
    cli.b 'batch size', longOpt: 'batch', args: 1
    def options = cli.parse(args)
    if (options && options.b && (!options.b.isInteger() || options.b < 0)) {
        Log.err 'Bad value for batch size'
        return false
    }
    options
}

options = parseOptions()

if (!options) {
    System.exit 1
}

def sql = SqlProducer.createFromEnv()

try {
    def csvLoader = new CsvLoader(
            sql: sql,
            table: options.table,
            file: options.file,
            columnNames: options.c ? options.c.split(',') as List : [],
            truncate: options.truncate as boolean,
            batchSize: options.b ? options.b as int : CsvLoader.DEFAULT_BATCH_SIZE,
            delimiter: options.delimiter ?: '\t',
            nullValue: options.null)
    csvLoader.prepareConnection()
    csvLoader.load()
} catch (Exception exception) {
    Log.err "CsvLoader threw with message '${exception.message}'; exiting with error code 1"
    exception.printStackTrace(System.err)
    System.exit 1
}

// vim: et sts=0 sw=4 ts=4 cindent cinoptions=(0,u0,U0
