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

import groovy.sql.Sql

def driver,
    jdbcUrl,
    username = 'tm_cz',
    password = 'tm_cz',
    rdbms = args[0]

def e = new Object()
e.metaClass.propertyMissing = System.&getenv
def err = System.err.&println

def colsArg = (args as List)[1]
def cols = e.COLUMNS ?
        Integer.parseInt(e.COLUMNS) :
        (colsArg ?
                Integer.parseInt(colsArg) :
                80)

if (cols < 80) {
	cols = 80
}

if (rdbms == 'oracle') {
    driver = 'oracle.jdbc.driver.OracleDriver'
    jdbcUrl = "jdbc:oracle:thin:@${e.ORAHOST}:${e.ORAPORT}:${e.ORASID}"
} else {
    driver = 'org.postgresql.Driver'
    def host = 'localhost'
    if (e.PGHOST && e.PGHOST[0] != '/') {
        host = e.PGHOST
    }
    jdbcUrl = "jdbc:postgresql://${host}:${e.PGPORT}/${e.PGDATABASE}"
}

Sql sql = Sql.newInstance jdbcUrl, username, password, driver

def row = sql.firstRow "SELECT MAX(job_id) AS job_id FROM cz_job_audit"
if (!row) {
    err "Could not find latest job id"
    System.exit 1
}
def jobId = row['JOB_ID']

def separator = new String(new char[cols]).replace('\0', '-');

def cross(List l1, List l2, Closure op) {
    if (l1.size() != l2.size()) {
        throw new IllegalArgumentException()
    }
    def ret = new ArrayList(l1.size())
    for (i in 0..(l1.size() - 1)) {
        ret[i] = op l1[i], l2[i]
    }
    ret
}
def colSize =  [ 15, 0, 4, 6, 20, 10]
def colAlign = [ '-', '-', '-', '', '', '']
colSize[1] = cols - colSize.sum() - (colSize.size() - 1) * 3
def printfSpec = cross(colSize, colAlign, { a, b -> "%${b}${a}.${a}s" }).join(' | ') + '\n'

printf printfSpec,
        'Procedure', 'Description', 'Stat', 'Recs', 'Date', 'Time spent'
println separator

sql.eachRow "SELECT procedure_name, step_desc, step_status, records_manipulated, job_date, time_elapsed_secs " +
        "FROM cz_job_audit WHERE job_id = $jobId ORDER BY seq_id ASC", {
    def itCop = (0..5).collect { n -> it[n] }
    if (!(itCop[4] instanceof Date)) {
        /* for Oracle it's an oracle.sql.TIMESTAMPLTZ */
        itCop[4] = itCop[4].dateValue(sql.connection).dateTimeString
    }
    if (itCop[0].size() > colSize[0]) {
        itCop[0] = itCop[0].substring(itCop[0].length() - colSize[0])
    }
    printf(printfSpec, *itCop)
}

def firstRow = true
sql.eachRow "SELECT error_message, error_backtrace FROM cz_job_error WHERE job_id = $jobId", {
    def colSizes = [ (int)(cols / 2) - 2,  (int)(cols / 2) - 1 ]
    if (firstRow) {
        println ''
        printf "%-${colSizes[0]}s | %-${colSizes[1]}s\n", 'Message', 'Location'
        println separator
        firstRow = false
    }
    printf "%-${colSizes[0]}s | %-${colSizes[1]}s\n", it[0], it[1]
}
