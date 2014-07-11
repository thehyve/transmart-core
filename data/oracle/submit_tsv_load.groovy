import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider
import inc.oracle.Log

def cli = new CliBuilder()
cli.t 'The (schema-qualified) table to load', args: 1, argName: 'table', longOpt: 'keep-file', required: true
cli.f 'The TSV file to load', args: 1, argName: 'file path', longOpt: 'tsv file', required: true
cli.h 'Show this help', longOpt: 'help'
cli.usage = '-t <table> -f <file>'

def options = cli.parse args
if (!options) {
    Log.err 'Invalid options'
    System.exit 1
}

def table = options.t
def file = new File(options.f)
if (!file.isFile()) {
    Log.err("No such file: $file")
    System.exit 1
}

LocalHost transport = new NettyTransportProvider()

def exitCode = new DataflowVariable()

localNode = new LocalNode(transport, {
    def connected = [:]
    addDiscoveryListener { node, operation ->
        if (operation == "connected") {
            connected[node.id] = node
        } else {
            connected.remove node.id
        }
    }

    loop { ->
        react { msg ->
            switch (msg.command) {
                case 'go ahead':
                    Log.out "Submitting load request for $table from $file"
                    connected[msg.id].mainActor << [
                            id: id,
                            command: 'load',
                            table: table,
                            file: file.absolutePath]
                    break
                case 'success':
                    Log.out "$table: Peer reported success"
                    exitCode << 0
                    terminate()
                    break
                case 'fail':
                    Log.err "$table: Peer reported failure"
                    exitCode << 1
                    terminate()
                    break
            }
        }
    }
})

localNode.mainActor.join()
localNode.disconnect()
transport.disconnect()
System.exit(exitCode.get())
