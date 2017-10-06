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
@Grab('org.codehaus.jackson:jackson-core-asl:1.9.13')
@Grab('org.codehaus.jackson:jackson-mapper-asl:1.9.13')
import groovy.sql.Sql
import groovyx.gpars.dataflow.DataflowVariable
import inc.oracle.*
import jsr166y.ForkJoinPool
import org.codehaus.jackson.map.ObjectReader

import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withExistingPool
import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

def cli = new CliBuilder()
cli.j 'Use the given number of connections', longOpt: 'n-conn', args: 1, argName: 'n'
cli.u 'Load objects from a user; can be specified multiple times',
        args: 1, argName: 'user', longOpt: 'user', required: true
cli._ 'Do not load regular user files', longOpt: 'no-regular'
cli._ 'Do not load synonym files (_synonyms.sql)', longOpt: 'no-synonyms'
cli._ 'Do not load grant files (_grants.sql)', longOpt: 'no-grants'
cli._ 'Do not load procedures', longOpt: 'no-procedures'
cli._ 'Do not load cross files (_cross.sql)', longOpt: 'no-cross'
cli._ 'Do not compile schemas', longOpt: 'no-compile'
cli._ 'Do not refresh views', longOpt: 'no-refresh-views'
cli._ 'Do not check for errors at the end', longOpt: 'no-final-error-check'

def options = cli.parse args
if (!options) {
    Log.err 'Invalid options'
    System.exit 1
}
def nConn = options.j ?: '1'
if (!nConn.isInteger() || nConn.toInteger() < 1) {
    Log.err "Invalid number of connections: $nConn"
    System.exit 1
} else if (options.g && options.c) {
    Log.err "The options -g and -c cannot be combined"
    System.exit 1
}
nConn = nConn.toInteger()

Integer loadFile(Sql sql, File file) {
    def i = 0
    file.withReader 'UTF-8', { reader ->
        new SqlSplitter(reader).each { statement ->
            i++
            try {
                sql.executeAndPrintWarnings statement
            } catch (SQLException exception) {
                Log.err "Failed loading statement $statement from $file"
                throw exception
            }
        }
    }
    Log.out "Loaded $file ($i)"

    i
}

def takeSql(BlockingQueue<Sql> sqls, Closure closure) {
    def sql = sqls.take()
    try {
        closure.call(sql)
    } finally {
        sqls.offer sql
    }

}

Integer loadFileParallel(ForkJoinPool pool, BlockingQueue<Sql> sqls, File file) {
    AtomicInteger i = new AtomicInteger(0)
    withExistingPool(pool) {
        file.withReader 'UTF-8', { reader ->
            def allStatements = []
            new SqlSplitter(reader).each { statement ->
                allStatements << statement
            }
            allStatements.eachParallel { statement ->
                i.incrementAndGet()
                takeSql(sqls) { Sql sql ->
                    try {
                        sql.executeAndPrintWarnings statement
                    } catch (SQLException exception) {
                        Log.err "Failed loading statement $statement from $file"
                        throw exception
                    }
                }
                Log.print '.'
            }
        }
    }
    System.out.println " Loaded $file ($i)"

    i.toInteger()
}

Closure createLoadFileInTransactionClosure(BlockingQueue<Sql> sqls) {
    { File f, String schemaToUse = null ->
        def sql = sqls.take()

        try {
            if (schemaToUse) {
                sql.setCurrentSchema schemaToUse
            }
            loadFile sql, f
            sql.commit()
            return true
        } catch (exception) {
            Log.err "Error loading file $f: $exception"
            try {
                sql.rollback()
            } catch (Exception rollbackException) {
                Log.err "Error rolling back transaction: $rollbackException"
            }
            return false
        } finally {
            if (schemaToUse) {
                sql.restoreCurrentSchema()
            }
            sqls.offer sql
        }
    }
}

/* resulting closure should be run inside a withPool block */
Map<File, DataflowVariable> loadMultiSchemaObjects(BlockingQueue<Sql> sqls,
                                                   FileDependencies fileDependencies,
                                                   Closure fileFilter) {

    def loadFileInTransaction = createLoadFileInTransactionClosure(sqls).asyncFun()

    Map<File, DataflowVariable> filePromiseMap = [:];

    fileDependencies.traverseDepthFirst { File curFile ->

        if (!fileFilter(curFile)) {
            return /* skip */
        }

        def parentPromises = fileDependencies[curFile].
                findAll { fileFilter(it) }.
                collect { parentFile -> filePromiseMap[parentFile] }

        assert parentPromises.every() /* no nulls */

        DataflowVariable promise = whenAllBound parentPromises, { List results ->
            def success = results.every()
            if (!success) {
                Log.warn "Skipping file $curFile because of dependency failure"
                return false
            }

            def user = curFile.path.replaceAll(/\/.*/, '').toUpperCase(Locale.ENGLISH)
            loadFileInTransaction curFile, user
        }, { Throwable exception ->
            //failure
            Log.warn "Skipping file $curFile because of uncaught exception in dependency"
            return false
        }

        filePromiseMap[curFile] = promise
    }

    filePromiseMap
}

/* returns list of failed files */
List<File> loadPerUserFile(BlockingQueue<Sql>sqls, List<String> users, String fileName) {
    Closure loadFileInTransaction = createLoadFileInTransactionClosure sqls
    def usersLower = users.collect { it.toLowerCase(Locale.ENGLISH) }

    usersLower.inject([]) { List<File> accum, String user ->
        File file = new File(user, fileName)
        file.exists() ?
            accum + file :
            accum
    }.collectParallel { File file ->
        [
                file: file,
                result: loadFileInTransaction(file)
        ]
    }.findAll {
        !it.result
    }.collect {
        it.file
    }
}

/* Generate the cross_grants.sql file */
void generateCrossGrantsFile(ItemRepository repos, File file) {
    repos.addGrantsForCrossDependencies()

    file.withWriter { writer ->
        repos.writeWithSorter { Item item, ItemRepository theRepo ->
            if (item.type == 'GRANT') {
                def anyCrossParent = theRepo.getParents(item).any { Item parent ->
                    theRepo.fileAssignments[parent]?.endsWith('/_cross.sql')
                }

                if (!anyCrossParent) {
                    ItemRepository.writeItem(item, writer)
                }
            }
        }
    }
}

/* go */
def sqls = new LinkedBlockingQueue(SqlProducer.createMultipleFromEnv(nConn, null, null, true))
List users = options.us.collect { it.toUpperCase(Locale.ENGLISH) }

withPool nConn, { pool ->
    ObjectReader objectReader = JacksonMapperProducer.mapper.reader ItemRepository

    def fileDependencies = new ConcurrentHashMap<String, FileDependencies>()

    // Calculate file dependencies and cross schema grants
    def repositories = new Vector<ItemRepository>()

    users.collectParallel { String user ->
        File userDir = new File(user.toLowerCase(Locale.ENGLISH))
        File itemsFile = new File(userDir, 'items.json')

        ItemRepository repository = objectReader.readValue itemsFile
        repositories << repository

        FileDependencies fileDeps = new FileDependencies()
        fileDeps.makeFileDependencies repository, user
        fileDependencies[user] = fileDeps
    }
    def joinedRepository = repositories.inject(new ItemRepository(), { accum, it ->
        accum + it
    })

    FileDependencies joinedFileDependencies =
            fileDependencies.values().inject {
                FileDependencies accum, FileDependencies deps ->
                    accum + deps
            }

    Closure getFailedFiles = { Map<File, DataflowVariable> filePromiseMap ->
        filePromiseMap.inject([]) { List accum, File file, DataflowVariable resultVar ->
            resultVar.get() ? accum : accum + file
        }
    }


    /* 1. all elements without cross-schema dependencies; no procedures */
    if (!options."no-regular") {
        List<File> failed = getFailedFiles(
                loadMultiSchemaObjects(sqls,
                        joinedFileDependencies,
                        { File sqlFile ->
                            if (sqlFile.parentFile.name == 'procedures') {
                                return false
                            }
                            if (sqlFile.name == '_cross.sql') {
                                if (joinedFileDependencies.getChildrenFor(sqlFile)) {
                                    Log.warn "$sqlFile won't be loaded but apparently it has dependent objects!"
                                    Log.warn "This should not happen. Things may very well fail!"
                                }
                                return false
                            }
                            true
                        }))

        if (failed) {
            Log.err "Failed loading failes: $failed"
            System.exit 1
        }
    }

    /* 2. load synonyms */
    if (!options."no-synonyms") {
        def failedFiles = loadPerUserFile sqls, users, '_synonyms.sql'
        if (failedFiles) {
            Log.err "Failed loading one or more synonyms files: $failedFiles"
            System.exit 1
        }
    }

    /* 3. load cross grants */
    if (!options."no-grants") {
        def file = new File('cross_grants.sql')
        try {
            generateCrossGrantsFile(joinedRepository, file)
            Log.out("Generated $file")
            def n = loadFileParallel(pool, sqls, file)
        } catch (SQLException sqlException) {
            Log.err "Failed loading cross grants file: $sqlException.message"
            System.exit 1
        }
    }

    /* 4. load procedures and cross */
    if (!(options."no-procedures" && options."no-cross")) {
        Map<File, DataflowVariable> filePromiseMap =
            loadMultiSchemaObjects(sqls,
                    joinedFileDependencies,
                    { File sqlFile ->
                        !options."no-procedures" && sqlFile.parentFile.name == 'procedures' ||
                                !options."no-cross" && sqlFile.name == '_cross.sql'
                    })

        List failedFiles = getFailedFiles filePromiseMap

        if (failedFiles) {
            Log.err "Failed loading one or more procedure or _cross.sql files: $failedFiles"
            System.exit 1
        }
    }

    /* 5. load explicit grants */
    if (!options."no-grants") {
        def failedFiles = loadPerUserFile sqls, users, '_grants.sql'
        if (failedFiles) {
            Log.err "Failed loading one or more grant files: $failedFiles"
            System.exit 1
        }
    }

    /* 6. recompile schemas */
    if (!options."no-compile") {
        Log.print 'Compiling schemas'
        // eachParallel causes errors sometimes:
        // ORA-20000: Unable to set values for index UTL_RECOMP_SORT_IDX1: does not exist or insufficient privileges
        users.each { String user ->
            takeSql(sqls) { Sql sql ->
                try {
                    sql.call "{call dbms_utility.compile_schema($user)}"
                    Log.print '.'
                } catch (SQLException exception) {
                    Log.err "Failed compiling schema for $user"
                    throw exception
                }
            }
        }
        Log.out ' Done.'
    }

    /* 7. refresh views */
    if (!options."no-refresh-views") {
        def statement = """
            BEGIN
                FOR the_view IN
                  (SELECT owner, mview_name
                    FROM DBA_MVIEWS
                     WHERE owner IN (${users.collect { "'$it'" }.join(', ')})
                     AND staleness <> 'FRESH')
                LOOP
                  DBMS_MVIEW.REFRESH(
                    the_view.owner || '.' || the_view.mview_name, method => '?');
                END LOOP;
            END;"""
        Log.print 'Refreshing views...'
        takeSql(sqls) { Sql sql ->
            sql.executeAndPrintWarnings statement.toString()
        }
        Log.out ' Done.'
    }

    /* 8. Report objects with errors */
    if (!options."no-final-error-check") {
        def statement = """
            SELECT DISTINCT type, owner, name
            FROM DBA_ERRORS
            WHERE owner IN (${users.collect { "'$it'" }.join(', ')})
            AND attribute = 'ERROR'""".toString()

        Log.out 'Checking for errors...'
        takeSql(sqls) { Sql sql ->
            sql.eachRow(statement) { ResultSet rs ->
                Log.warn "${rs.getString(1)} " +
                        "${rs.getString(2)}.${rs.getString(3)} has errors"
            }
        }
    }
}

Log.out 'All done.'
