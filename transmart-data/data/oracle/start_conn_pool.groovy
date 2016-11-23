import groovy.sql.Sql
import groovyx.gpars.actor.Actor
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider
import inc.oracle.CsvLoader
import inc.oracle.Log
import inc.oracle.SqlProducer

import java.nio.channels.FileLock
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Phaser

import static groovyx.gpars.dataflow.Dataflow.task

def cli = new CliBuilder()
cli.j 'Use the given number of connections', longOpt: 'n-conn', args: 1, argName: 'n'
cli.f 'Lock file. If specified, will try to acquire a lock on this file', args: 1, argName: 'file', longOpt: 'lock-file'
cli.h 'Show this help', longOpt: 'help'
cli.x 'Exit on error', longOpt: 'exit-on-error'
cli.usage = '[-j <num conn>] [-f <file>]'

def options = cli.parse args
if (!options) {
    Log.err 'Invalid options'
    System.exit 1
}

def nConn = options.j ?: '4'
if (!nConn.isInteger() || nConn.toInteger() < 1) {
    Log.err "Invalid number of connections: $nConn"
    System.exit 1
}
nConn = nConn.toInteger()

FileLock lock

def lockFile(file) {
    File f = new File(file)
    if (!f.isFile()) {
        if (!f.createNewFile()) {
            Log.err "Could not create file ${f.absolutePath}"
            System.exit 1
        }
    }
    FileLock lock = new FileOutputStream(f).channel.tryLock()
    if (lock == null) {
        Log.err "Could not obtain lock on '${f.absolutePath}', exiting"
        System.exit 1
    }
    lock
}
if (options.f) {
    lock = lockFile(options.f)
}

class Sync {
    Phaser phaser = new Phaser(0) {
        boolean onAdvance(int phase, int registeredParties) {
            false
        }
    }
    volatile int arrivalPhase = -1

    void register() {
        arrivalPhase = phaser.register()
    }

    void deregister() {
        phaser.arriveAndDeregister()
    }

    void await() {
        phaser.awaitAdvance(arrivalPhase)
    }
}

def sync = new Sync()

LinkedBlockingQueue<CsvLoader> csvLoaders
LocalNode localNode

PGroup group = new NonDaemonPGroup(nConn + 3)

final Actor launcherActor = group.actor {
    def sqls = SqlProducer.createMultipleFromEnv(nConn, null, null, true)
    Log.out "Established ${sqls.size()} database connections"

    csvLoaders = new LinkedBlockingQueue(
            sqls.collect { Sql sql ->
                group.task {
                    CsvLoader ret = new CsvLoader(sql: sql, quiet: true)
                    ret.prepareConnection()
                    ret
                }
            }*.get())

    Log.out "Prepared ${csvLoaders.size()} database connections"

    loop {
        react { pair ->
            def msg = pair[0]
            def remoteActor = pair[1]

            sync.register()
            doTable(msg.table, msg.file, csvLoaders).then({ result ->
                Log.out "Finished ${msg.table} ($result lines)"
                remoteActor << [command: 'success', id: localNode.id]
                sync.deregister()
            }, {
                remoteActor << [command: 'fail', id: localNode.id]
                sync.deregister()
                if (options.x) {
                    localNode.mainActor << [command: 'die']
                    Log.warn "Terminating due to exception"
                    stop()
                }
            })
        }
    }
}

DataflowVariable doTable(String table,
                         String file,
                         LinkedBlockingQueue<CsvLoader> loaders) {
    task {
        CsvLoader loader = loaders.take()
        try {
            Log.out "Loading data for $table from $file"
            loader.table = table
            loader.file = file
            loader.load()
            if (loader.skippedLines) {
                Log.warn "$table: Skipped ${loader.skippedLines} from $file"
            }

            return loader.insertedLines
        } catch (Exception e) {
            Log.err "Failed loading data for ${loader.table}: ${e}"
            e.printStackTrace(System.err)
            throw e
        } finally {
            loaders.offer(loader)
        }
    }
}

LocalHost transport = new NettyTransportProvider()

localNode = new LocalNode(transport, {
    def connected = [:]
    addDiscoveryListener { node, operation ->
        Log.out "${node.id} $operation"

        if (operation == "connected") {
            connected[node.id] = node
            node.mainActor << [command: 'go ahead', id: id]
        } else {
            connected.remove node.id
        }
    }

    Log.out "Waiting for connections"
    loop { ->
        react { msg ->
            switch (msg.command) {
                case 'load':
                    launcherActor << [msg, connected[msg.id].mainActor]
                    break
                case 'die':
                    launcherActor.stop()
                    Log.out 'Got poison pill message'
                    stop()
                    break
                default:
                    Log.err("Unknown message: $msg.command")
            }
        }
    }
})

localNode.mainActor.join()

// don't shutdown before all the messages have been handled
// shutting down immediately would prevent the "then" tasks
// from being submitted
sync.await()
group.shutdown()
localNode.disconnect()
transport.disconnect()

if (lock) {
    lock.release()
}

SqlProducer.closeConnections(sqls)
Log.out 'Closed all database connections. Finished'
System.exit(0)
