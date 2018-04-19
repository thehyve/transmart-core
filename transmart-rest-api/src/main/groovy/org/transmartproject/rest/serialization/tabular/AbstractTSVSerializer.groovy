package org.transmartproject.rest.serialization.tabular

import com.opencsv.CSVWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.users.User

import java.text.SimpleDateFormat
import java.util.zip.ZipOutputStream

@CompileStatic
class AbstractTSVSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy hh:mm")

    final User user
    final ZipOutputStream zipOutStream

    AbstractTSVSerializer(User user, OutputStream outputStream) {
        this.user = user
        this.zipOutStream = new ZipOutputStream(outputStream)
    }

    protected String[] formatRowValues(List<? extends Object> valuesRow) {
        def s = new String[valuesRow.size()]

        for (int i=0; i<s.length; i++) {
            def value = valuesRow[i]
            if (value == null) continue
            if (value instanceof Date) {
                synchronized (DATE_FORMAT) {
                    s[i] = DATE_FORMAT.format(value)
                }
            } else {
                s[i] = value.toString()
            }
        }

        return s
    }

    void writeRow(CSVWriter csvWriter, List row) {
        csvWriter.writeNext(formatRowValues(row))
    }

    CSVWriter getCSVWriter() {
        new CSVWriter(new OutputStreamWriter(zipOutStream), COLUMN_SEPARATOR)
    }
}
