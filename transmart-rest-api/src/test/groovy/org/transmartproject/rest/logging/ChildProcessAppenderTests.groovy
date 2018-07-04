/*
 * Copyright © 2013-2016 The Hyve B.V.
 *
 * This file is part of Transmart.
 *
 * Transmart is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Transmart.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.nio.charset.StandardCharsets

import static org.apache.commons.io.FileUtils.readFileToString
import static org.apache.commons.io.FileUtils.writeStringToFile
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import static org.transmartproject.rest.logging.ChildProcessAppender.ChildFailedException

class ChildProcessAppenderTests extends Specification {
   
    @Rule
    TemporaryFolder temp = new TemporaryFolder()
    
    static String TESTSTRING = "hello world! testing ChildProcessAppender\n"

    static sh(cmd) { return ['sh', '-c', cmd] }
    // escape shell strings, based on http://stackoverflow.com/a/1250279/264177
    static path(File file) { "'${file.path.replaceAll("'", "'\"'\"'")}'" }

    static void waitForChild(ChildProcessAppender a) {
        a.input?.close()
        a.process.waitFor()
    }
    
    void testLoggingEvent() {
        setup:
        File output = temp.newFile('output')
        def p = new ChildProcessAppender(command: sh("cat >"+path(output)))
        Logger logger=(Logger)LoggerFactory.getLogger(this.getClass())
        String message = '{"foo": "bar", "baz": "quux"}'
        LoggingEvent e = new LoggingEvent("", logger, Level.DEBUG, message, null, null)

        when:
        p.start()
        p.doAppend(e)
        waitForChild(p)
        p.stop()

        then:
        readFileToString(output).contains("message=$message")
    }

    void testOutput() {
        setup:
        File output = temp.newFile('output')
        def p = new ChildProcessAppender(command: sh("cat > "+path(output)))
        
        when:
        p.write(TESTSTRING)
        waitForChild(p)
        
        then:
        readFileToString(output) == TESTSTRING
    }
    
    void testFail() {
        setup:
        def p = new ChildProcessAppender(command: ["false"], restartLimit: 3, throwOnFailure: true)
       
        when:
        p.write(TESTSTRING)
        waitForChild(p)
        p.write(TESTSTRING)
        waitForChild(p)
        p.write(TESTSTRING)
        waitForChild(p)
        p.write(TESTSTRING)
        waitForChild(p)
        
        then:
        thrown ChildFailedException
    }

    void testRestart() {
        when:
        def output = do_testRestart(3, 15)
        then:
        // restarting a child process may lose some messages, so we can not be sure of how many copies of TESTSTRING
        // there are
        assertThat readFileToString(output), containsString(TESTSTRING)
    }
    
    void testRestartLimit() {
        when:
        do_testRestart(5, 3)
        then:
        thrown ChildFailedException
    }

    File do_testRestart(int restarts, int limit) {
        File runcount = temp.newFile('count')
        File output = temp.newFile('output')
        writeStringToFile(runcount, '0\n', StandardCharsets.US_ASCII)
        String command = """
            countfile=${path(runcount)}
            count=`cat "\$countfile"`
            if [ "\$count" -le ${restarts} ]
            then
                echo `expr "\$count" + 1` > "\$countfile"
                exit
            else
                cat > ${path(output)}
            fi"""
        def p = new ChildProcessAppender(command: sh(command), restartLimit: limit, throwOnFailure: true)
        int count = -1
        while (count <= restarts) {
            p.write(TESTSTRING)
            def countstr = readFileToString(runcount).trim()
            if (countstr) count = Integer.parseInt(countstr)
        }
        p.write(TESTSTRING)
        p.stop()
        waitForChild(p)

        return output
    }
}
