@Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.13')
import groovy.sql.Sql
import groovyx.gpars.dataflow.DataflowVariable
import inc.*
import org.codehaus.jackson.map.ObjectReader

import java.sql.SQLException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

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
    System.out.println "Loaded $file ($i)"

    i
}

/* we'll be loading each schema inside a transaction for consistency/
 * Consequently, we cannot distribute the DDL statements in a uniform
 * fashion between the threads/connections.
 *
 * Each connection will handle only one schema (at a time) and only one
 * connection will handle one schema.
 *
 * XXX:
 * This approach is actually useless because oracle cannot rollback
 * DDL statements and implicitly commits after each one is issued.
 * So we might as well use loadMultiSchemaObjects to load the
 * these objects as well since it will distribute the statements between
 * the connections better and finish sooner.
 */
Closure loadSchemaNoCrossClosure(BlockingQueue<Sql> sqls,
                                 String owner /* uppercase */,
                                 FileDependencies fileDependencies,
                                 Boolean doProcedures) {
    { ->
        Sql sql = sqls.take()
        sql.setCurrentSchema owner

        try {
            fileDependencies.traverseDepthFirst { File file ->
                if (file.parentFile.name == 'procedures' && !doProcedures) {
                    return
                }
                if (file.name == '_cross.sql') {
                    if (fileDependencies.getChildrenFor(file)) {
                        Log.err "_cross.sql won't be loaded but apparently it has dependent objects!"
                        Log.err "This should not happen. Things may very well fail!"
                    }
                    return
                }

                loadFile sql, file
            }
            sql.commit()
            return true
        } catch (exception) {
            try {
                sql.rollback()
            } catch (rollbackException) {
                Log.err "User $owner: error rolling back transaction: $rollbackException"
            }
            Log.err "User $owner: error loading DDL: $exception\n"

            return false
        } finally {
            sql.restoreCurrentSchema()
            sqls.offer sql //give it back
        }
    }
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
                Log.err "Skipping file $curFile because of dependency failure"
                return false
            }

            def user = curFile.path.replaceAll(/\/.*/, '').toUpperCase(Locale.ENGLISH)
            loadFileInTransaction curFile, user
        }, { Throwable exception ->
            //failure
            Log.err "Skipping file $curFile because of uncaught exception in dependency"
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

/* go */
def sqls = new LinkedBlockingQueue(SqlProducer.createMultipleFromEnv(nConn, null, null, true))
List users = options.us.collect { it.toUpperCase(Locale.ENGLISH) }

withPool nConn, {
    def runClosuresFromProducer = {
        Closure closureProducer,
        Map<String, List> argumentsList -> /* user: list of arguments */

        List<String> failedUsers = argumentsList.collectEntries { user, args ->
            DataflowVariable resultVar = closureProducer(*args).asyncFun().call()
            [ (user): resultVar ]
        }.inject([]) { List list, String user, DataflowVariable variable ->
            variable.get() ?
                    list /* success */ :
                    list + user /* failure */
        }

        failedUsers
    }

    ObjectReader objectReader = JacksonMapperProducer.mapper.reader ItemRepository

    def fileDependencies = new HashMap<String, FileDependencies>()

    List argumentsList = users.collectParallel { String user ->
        File userDir = new File(user.toLowerCase(Locale.ENGLISH))
        File itemsFile = new File(userDir, 'items.json')

        ItemRepository repository = objectReader.readValue itemsFile
        fileDependencies[user] = new FileDependencies()
        fileDependencies[user].makeFileDependencies repository, user

        [
                sqls,
                user,
                fileDependencies[user],
                false
        ]
    }

    /* 1. all elements without cross-schema dependencies; no procedures */
    if (!options."no-regular") {
        List<String> failedUsers = runClosuresFromProducer(
                this.&loadSchemaNoCrossClosure,
                argumentsList.collectEntries { [ (it[1]): it ] })
        if (failedUsers) {
            Log.err "Failed loading regular objects for one or more users: $failedUsers"
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

    /* 3. load grants */
    if (!options."no-grants") {
        def failedFiles = loadPerUserFile sqls, users, '_grants.sql'
        if (failedFiles) {
            Log.err "Failed loading one or more grant files: $failedFiles"
            System.exit 1
        }
    }

    /* 4. load procedures and cross */
    if (!(options."no-procedures" && options."no-cross")) {
        FileDependencies joinedFileDependencies =
            fileDependencies.values().inject {
                FileDependencies accum, FileDependencies deps ->

                accum + deps
            }

        Map<File, DataflowVariable> filePromiseMap =
            loadMultiSchemaObjects(sqls,
                    joinedFileDependencies,
                    { File sqlFile ->
                        !options."no-procedures" && sqlFile.parentFile.name == 'procedures' ||
                                !options."no-cross" && sqlFile.name == '_cross.sql'
                    })

        List failedFiles = filePromiseMap.inject([]) { List accum, File file, DataflowVariable resultVar ->
            resultVar.get() ? accum : accum + file
        }

        if (failedFiles) {
            Log.err "Failed loading one or more procedure or _cross.sql files: $failedFiles"
            System.exit 1
        }
    }

}

Log.out 'Done'
