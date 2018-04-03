package org.transmartproject.rest.serialization

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.SortOrder
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.DataTableRow
import org.transmartproject.core.multidimquery.Dimension

import java.time.Instant

@CompileStatic
class DataTableSerializer {

    private JsonWriter writer
    private DataTable table

    static void write(DataTable table, OutputStream out) {
        write([:], table, out)
    }

    static void write(Map args, DataTable table, OutputStream out) {
        new DataTableSerializer().writeData(args, table, out)
    }

    void writeColumnHeaders() {
        writer.name('column_headers').beginArray()
        for(int i=0; i<table.columnDimensions.size(); i++) {
            def dim = table.columnDimensions[i]
            writer.beginObject()

            writer.name('dimension').value(dim.name)

            if(dim.elementsSerializable) {
                writer.name('elements').beginArray()
                for(def columnHeader : table.columnKeys) {
                    writeValue(columnHeader.elements[i])
                }
                writer.endArray()
            } else {
                writer.name('keys').beginArray()
                for(def columnHeader : table.columnKeys) {
                    writeValue(dim.getKey(columnHeader.elements[i]))
                }
                writer.endArray()
            }
            writer.endObject()
        }

        writer.endArray()
    }

    void writeRows() {
        writer.name('rows').beginArray()
        for(DataTableRow rowHeader : table.rowKeys) {
            writeRow(rowHeader)
        }
        writer.endArray()
    }

    void writeRow(DataTableRow rowHeader) {
        writer.beginObject()

        writer.name('dimensions').beginArray()
        for(int i=0; i<table.rowDimensions.size(); i++) {
            def rowDim = table.rowDimensions[i]
            def element = rowHeader.elements[i]
            writer.beginObject()
            writer.name('dimension').value(rowDim.name)
            if(rowDim.elementsSerializable) {
                writer.name('element')
                writeValue(element)
            } else {
                writer.name('key')
                writeValue(rowDim.getKey(element))
            }
            writer.endObject()
        }
        writer.endArray()

        writer.name('row').beginArray()
        def row = table.row(rowHeader)
        for(def column : table.columnKeys) {
            def cell = row[column]
            writeValue(cell)
        }
        writer.endArray()

        writer.endObject()
    }

    void writeDimensions(String type, List<Dimension> dimensions) {
        writer.name("${type}_dimensions").beginArray()
        for(def dim : dimensions) {
            writer.beginObject()
            writer.name('name').value(dim.name)
            if(!dim.elementsSerializable) {
                writer.name('elements').beginObject()
                for(def element: table.hypercube.dimensionElements(dim)) {
                    writer.name(dim.getKey(element).toString())
                    Map value = (Map) dim.asSerializable(element)
                    value.label = dim.getKey(element)
                    writeValue(value)
                }
                writer.endObject()
            }
            writer.endObject()
        }
        writer.endArray()
    }

    void writeSorting(Map<Dimension, SortOrder> userSorting) {
        writer.name('sorting').beginArray()
        for(def entry : table.sort) {
            writer.beginObject()
            writer.name('dimension').value(entry.key.name)
            writer.name('order').value(entry.value.string())
            if(entry.key in userSorting) {
                writer.name('user_requested').value(true)
            }
            writer.endObject()
        }
        writer.endArray()
    }

    void writeOtherKeys() {
        writer.name('offset').value(table.offset)
        if(table.totalRowCount != null) {
            writer.name('row count').value(table.totalRowCount)
        }
    }

    private void writeData(Map args, DataTable table, OutputStream out) {
        this.writer = new JsonWriter(new BufferedWriter(
                new OutputStreamWriter(out),
                // large 32k chars buffer to reduce overhead
                32*1024))
        this.table = table

        writer.beginObject()
        writeColumnHeaders()
        writeRows()
        writeDimensions('row', table.rowDimensions)
        writeDimensions('column', table.columnDimensions)
        writeSorting((Map) args.userSorting ?: [:])
        writeOtherKeys()

        writer.endObject()
    }

    protected void writeValue(Object value) {
        if (value == null) {
            writer.nullValue()
        } else if (value instanceof String) {
            writer.value((String) value)
        } else if (value instanceof Date) {
            def time = Instant.ofEpochMilli(((Date) value).time).toString()
            writer.value(time)
        } else if (value instanceof Number) {
            writer.value((Number) value)
        } else if (value instanceof Map) {
            Map obj = (Map) value
            writer.beginObject()
            for(Map.Entry e : obj) {
                writer.name((String) e.key)
                writeValue(e.value)
            }
            writer.endObject()
        } else {
            throw new IllegalArgumentException("Expected a String, Number, Date or Map, got a ${value.class}: $value")
        }
    }
}
