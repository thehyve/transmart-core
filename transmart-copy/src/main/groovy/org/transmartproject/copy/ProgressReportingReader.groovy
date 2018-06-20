package org.transmartproject.copy

import groovy.transform.CompileStatic
import me.tongfei.progressbar.ProgressBar

/**
 * Enables progress reporting on the command line based on number of lines read.
 */
@CompileStatic
class ProgressReportingReader extends LineNumberReader {

    ProgressBar progressBar

    ProgressReportingReader(Reader reader, String title, int numberOfLines) {
        super(reader)
        this.progressBar = new ProgressBar("Loading into ${title}", numberOfLines)
        this.progressBar.start()
    }

    @Override
    int read(char[] cbuf, int off, int len) throws IOException {
        int before = lineNumber
        int result = super.read(cbuf, off, len)
        int after = lineNumber
        if (before != after) {
            this.progressBar.stepTo(after)
        }
        result
    }

}
