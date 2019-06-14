package org.transmartproject.copy

import spock.lang.Specification

/**
 * Test tranmsart-copy command line interface
 */
class CopyCliSpec extends Specification {

    def 'returns non zero exist status when no required args provided'() {
        expect:
        exec(Copy, []) > 0
    }

    def 'returns zero exist status when asked for help'() {
        expect:
        exec(Copy, ['--help']) == 0
    }

    static int exec(Class cls, List<String> args) {
        String javaBin = [System.getProperty("java.home"), "bin", "java"].join(File.separator)
        String classpath = System.getProperty("java.class.path")

        List<String> cmd = [javaBin, "-cp", classpath, cls.canonicalName] + args
        ProcessBuilder builder = new ProcessBuilder(cmd)

        Process process = builder.start()
        process.waitFor()
        process.exitValue()
    }
}
