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


import groovy.transform.ToString
@Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0')
import groovyx.gpars.GParsPool
@Grab(group='commons-net', module='commons-net', version='3.3')
import org.apache.commons.net.ftp.FTPClient
import groovy.transform.Canonical
import groovy.transform.InheritConstructors
import org.apache.commons.net.ftp.FTPReply
import groovy.util.logging.Slf4j
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

@Slf4j
class Script {

    static run(args) {
        def cli = new CliBuilder()
        cli.f 'Use the feeds specified in this file; can be specified multiple times',
        args: 1, argName: 'file', longOpt: 'feed-list', required: true
        cli.o 'Output file', args: 1, argName: 'file', longOpt: 'out', required: true
        cli.h 'Show this help', longOpt: 'help'
        cli.usage = '[-j <num conn>] -f <file> [-f <file> ...]'

        def options = cli.parse args
        if ( !options ) {
            log.error 'Invalid options'
            System.exit 1
        }
        final int NUMBER_OF_THREADS = 4

        List<DataSet> dataSets
        GParsPool.withPool ( NUMBER_OF_THREADS ) {
            def feeds = new FeedFactory(options.fs).createFeeds()
            dataSets = feeds.collect { feed ->
                try {
                    def result = feed.fetchDataSets()
                    log.info "Got ${result.size()} data sets from $feed"
                    result
                } catch (Exception e) {
                    log.error "Error fetching data sets for feed $feed: ${e.message}"
                    System.exit 1
                }
            }.flatten()
        }

        def groupedDataSets = dataSets.groupBy {
            [it.study, it.type]
        }
        log.info "Found  ${groupedDataSets.size()}  unique data sets"
        dataSets = groupedDataSets.collect {
            List<String> key, List<DataSet> list ->
            list
        }

        new File ( options.o ).withWriter {
            writer ->
            dataSets.each {
                writer.write "${it[0].study} ${it[0].type} " +
                        "${it*.URL*.toExternalForm().join(" ")}\n"
            }
        }
    }
}

Script.run(args)

interface Feed {
    Set<DataSet> fetchDataSets()
}

@Canonical(excludes = 'URL')
class DataSet {
    String baseLocation
    String relativePath
    String study
    String type

    URL getURL() {
        new URL(baseLocation + relativePath)
    }
}

class FeedFactory {
    List<File> files
    FeedFactory(List<String> files) {
        this.files = files.collect { new File(it) }
    }

    final static Map<String, Class> FEED_TYPES = [
            'http-index': HttpIndexFeed,
            'ftp-flat': FtpFlatFeed,
    ]

    Set<Feed> createFeeds() {
        files.collect {
            def result = []
            it.eachLine { line ->
                List<String> split = line.split(/\s+/, 2) as List
                if (split.size() != 2) {
                    throw new InvalidDataException(
                            "Invalid line in feed list: $line")
                }
                if (!FEED_TYPES[split[0]]) {
                    throw new InvalidDataException(
                            "Unknown feed type: ${split[0]}")
                }
                result << FEED_TYPES[split[0]].newInstance(split[1])
            }
            result
        }.flatten() as Set
    }
}

@InheritConstructors
class InvalidDataException extends RuntimeException {}

@ToString(includes = 'index')
class HttpIndexFeed implements Feed {
    URL index
    String baseLocation

    HttpIndexFeed(String data) {
        index = new URL(data)
        def urlString = index as String
        baseLocation = urlString.substring(0, urlString.lastIndexOf('/') + 1)
    }

    @Override
    Set<DataSet> fetchDataSets() {
        def ret = [] as Set

        def is = index.openStream()
        try {
            // assumes UTF-8
            def reader = new BufferedReader(new InputStreamReader(is, 'UTF-8'))
            def line
            while ((line = reader.readLine()) != null) {
                if (line =~ /^\s*$/ || line =~ /^#/) {
                    continue
                }
                ret << parseLine(line)
            }
        } finally {
            is.close()
        }

        ret
    }

    DataSet parseLine(String line) {
        def split = line.split(/\s+/, 3) as List
        if (split.size() != 3) {
            throw new InvalidDataException("Invalid line from feed $index: $line")
        }
        new DataSet(
                baseLocation: baseLocation,
                relativePath: split[2],
                study: split[0],
                type: split[1]
        )
    }
}

@ToString(includes = 'url')
class FtpFlatFeed implements Feed {
    URL url
    FtpFlatFeed(String data) {
        if (data[-1] != '/') {
            data += '/'
        }
        url = new URL(data)
        if (url.protocol != 'ftp') {
            throw new InvalidDataException("Expected ftp url; got $data")
        }
    }

    @Override
    Set<DataSet> fetchDataSets() {
        def ftp = new FTPClient()
        ftp.connect(url.host, url.port == -1 ? url.defaultPort : url.port)
        checkReply ftp, 'connect'

        ftp.enterLocalPassiveMode()

        def userInfo = url.userInfo ?: 'anonymous:ftp@example.com'

        try {
            if (userInfo) {
                def userPass = userInfo.split(':', 2) as List
                ftp.login(userPass[0], userPass[1] ?: '')
                checkReply ftp, 'login'
            }
            ftp.cwd(url.path)
            checkReply ftp, 'cwd'


            def names = ftp.listNames()
            if (names == null) {
                throw new InvalidDataException("Error listing directory for $this")
            }

            names.collect { name ->
                def matcher = name =~ /^(.+)_(.+)\.tar\.xz$/
                if (!matcher.matches()) {
                    return
                }

                new DataSet(
                        baseLocation: url.toExternalForm(),
                        relativePath: name,
                        study: matcher.group(1),
                        type: matcher.group(2),
                )
            }.findAll() as Set
        } finally {
            ftp.disconnect()
        }
    }

    void checkReply(FTPClient client, String stage) {
        def reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            throw new InvalidDataException(
                    "Got non-positive completion for feed $url after $stage: $reply")
        }
    }
}
