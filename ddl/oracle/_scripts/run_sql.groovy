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
import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.SelectResult
import groovyx.gpars.group.NonDaemonPGroup
import inc.oracle.Log
import inc.oracle.SqlProducer
import inc.oracle.SqlSplitter

import java.sql.SQLException
import java.util.concurrent.LinkedBlockingQueue

import static groovyx.gpars.dataflow.Dataflow.select
import static groovyx.gpars.GParsPool.withPool


def cli = new CliBuilder()
cli.j 'Use the given number of connections (default: min(number of files, 5)). ' +
        'Will always use only one if --sequential or --transaction are passed',
        longOpt: 'n-conn', args: 1, argName: 'n'
cli.f 'File to load. Defaults to stdin. Can be specified multiple times', longOpt: 'file', args: 1, argName: 'file'
cli.c 'Load not just files, but also files\' statements concurrently', longOpt: 'files-concurrently'
cli._ 'Load files sequentially', longOpt: 'sequential'
cli.u 'Login as the specified user instead of $ORAUSER', longOpt: 'user', args: 1
cli.p 'Login with the specified password instead of $ORAPASSWORD', longOpt: 'password', args: 1, argName: 'pass'
cli.s 'Change to the specified schema', longOpt: 'schema', args: 1, argName: 'user'
cli.t 'Run everything in transactions. Stops and rolls back in case of error', longOpt: 'transaction'
cli.h 'Show this help', longOpt: 'help'
cli.usage = '[-f <file1> [-f <file2> ...]] [-j <n>] [-t] [OTHER OPTIONS]'

def options = cli.parse args
if (!options) {
    Log.err 'Invalid options'
    System.exit 1
}
if (options.h) {
    cli.usage()
    return
}
def nConn = options.j ?: "${options.files ? options.files.size() : 1}"
if (!nConn.isInteger() || nConn.toInteger() < 1) {
    Log.err "Invalid number of connections: $nConn"
    System.exit 1
}
def sequential = options.sequential
if (sequential) {
    nConn = '1'
}
nConn = nConn.toInteger()

class IteratorWhile<T> implements Iterator<T> {

    private Closure c
    private T nextElement

    IteratorWhile(Closure c) {
        this.c = c
        next()
    }

    boolean hasNext() {
        nextElement != null
    }

    T next() {
        def ret = nextElement
        nextElement = c.call()
        ret
    }

    void remove() {
        throw UnsupportedOperationException()
    }
}

def sqls = new LinkedBlockingQueue(
        SqlProducer.createMultipleFromEnv(nConn, options.user ?: null, options.password ?: null, true))
if (options.schema) {
    withPool sqls.size(), {
        sqls.eachParallel {
            it.setCurrentSchema options.schema.toUpperCase(Locale.ENGLISH)
        }
    }
}

Map<File, Reader> readers
if (options.files) {
    readers = options.files.collectEntries {
        [ (new File(it)): new InputStreamReader(new FileInputStream(new File(it)), 'UTF-8') ]
    }
} else {
    readers = [ (new File('(stdin)')): new InputStreamReader(System.in, 'UTF-8') ]
}


Boolean runStatements(File file,
                      Sql sql,
                      Iterator<String> statements,
                      Boolean returnOnError) {

    def result = true

    while (statements.hasNext()) {
        String sqlStatement = statements.next()
        System.out.print '.'
        try {
            sql.executeAndPrintWarnings sqlStatement
        } catch (SQLException exception) {
            Log.err "Failed executing statement $sqlStatement because $exception.message (file $file)"
            result = false
            if (returnOnError) {
                break
            }
        }
    }

    result
}

if (sequential) {
    Sql sql = sqls.take()
    readers.each { File file, Reader reader ->
        Log.out "Loading file $file..."
        def splitter = new SqlSplitter(reader)
        def result = runStatements(file,
                sql,
                new IteratorWhile({ -> splitter.nextStatement }),
                options.transaction)
        if (!result && options.transaction) {
            sql.rollback()
            System.exit 1
        }
    }
    sql.commit()
    Log.out "Done"
    System.exit 0
}


def group = new NonDaemonPGroup(nConn + 1)
def queue = new DataflowQueue()
def shutdownBroadcast = new DataflowBroadcast()

def processClosure = { sql ->
    def result = true
    def sel = select(shutdownBroadcast.createReadChannel(), queue)

    while (true) {
        SelectResult selectResult = sel.prioritySelect()
        if (selectResult.index == 0) {
            // message in shutdown channel
            break
        }

        List fileStatementsPair = selectResult.value
        if (fileStatementsPair == null) {
            //EOF
            shutdownBroadcast << true
            break
        }


        File file = fileStatementsPair[0]
        Iterator<String> statements = fileStatementsPair[1]

        result = runStatements(file, sql, statements, options.transaction) && result

        if (!result && options.transaction) {
            shutdownBroadcast << false
        }

        if (!options.transaction) {
            sql.commit()
        }
    }

    return result
}

List<Promise> promises = (1..nConn).collect {
    group.task { //maybe make each statement a task would be simpler...
        Sql sql = sqls.take()
        try {
            processClosure.call sql
        } finally {
            sqls.offer sql
        }
    }
}

def distributionPromise = group.task {
    readers.each { File file, Reader reader ->
        def splitter = new SqlSplitter(reader)

        if (options.'files-concurrently') {
            while ((sqlStatement = splitter.nextStatement) != null) {
                queue << [ file, [ sqlStatement ].iterator() ]
            }
        } else {
            queue << [ file, new IteratorWhile({ -> splitter.nextStatement }) ]
        }
    }
    queue << null /* signals end of stream */
}

distributionPromise.join()
def allSuccessful = promises.collect { it.get() }.every()

group.shutdown()

SqlProducer.closeConnections(sqls) { Sql sql ->
    if (!allSuccessful && options.transaction) {
        sql.rollback()
    }
}

if (!allSuccessful) {
    Log.out "Done with errors"
    System.exit 1
} else {
    Log.out "Done"
}
