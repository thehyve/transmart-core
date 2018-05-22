package org.transmartproject.rest.serialization

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.CrossTableRow

@CompileStatic
class CrossTableSerializer {

    protected JsonWriter writer

    protected void writeCounts(final List<Long> counts) {
        writer.beginArray()
        for(long count: counts) {
            writer.value(count)
        }
        writer.endArray()
    }

    protected void writeRows(final List<CrossTableRow> rows) {
        writer.beginArray()
        for(CrossTableRow row: rows) {
            writeCounts(row.counts)
        }
        writer.endArray()
    }

    /**
     * Writes the cross table rows to JSON.
     *
     * @param rows the cross table rows to serialise.
     * @param out the stream to write to.
     */
    void write(final List<CrossTableRow> rows, OutputStream out) {
        this.writer = new JsonWriter(new PrintWriter(new BufferedOutputStream(out)))
        this.writer.indent = ''
        writer.beginObject()
        writer.name('rows')
        writeRows(rows)
        writer.endObject()
        writer.flush()
    }
}
