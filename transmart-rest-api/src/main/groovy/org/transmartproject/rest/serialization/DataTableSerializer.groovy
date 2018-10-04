package org.transmartproject.rest.serialization

import com.google.common.collect.Table
import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.multidimquery.hypercube.Dimension

import java.time.Instant
import java.util.stream.Collectors

@CompileStatic
class DataTableSerializer {

    private JsonWriter writer
    private PagingDataTable table

    static void write(PagingDataTable table, OutputStream out) {
        new DataTableSerializer(table, out).writeData()
    }

    DataTableSerializer(PagingDataTable table, OutputStream out) {
        this.writer = new JsonWriter(new BufferedWriter(
                new OutputStreamWriter(out),
                // large 32k chars buffer to reduce overhead
                32*1024))
        this.table = table
    }

    protected void writeColumnHeader(Dimension dimension, int i) {
        writer.beginObject()
        writer.name('dimension').value(dimension.name)
        if (dimension.elementsSerializable) {
            writer.name('elements').beginArray()
            for(def columnHeader : table.columnKeys) {
                writeValue(columnHeader.elements[i])
            }
            writer.endArray()
        } else {
            writer.name('keys').beginArray()
            for(def columnHeader : table.columnKeys) {
                writeValue(dimension.getKey(columnHeader.elements[i]))
            }
            writer.endArray()
        }
        writer.endObject()
    }

    protected void writeColumnHeaders() {
        writer.name('columnHeaders').beginArray()
        for (int i=0; i<table.columnDimensions.size(); i++) {
            def dimension = table.columnDimensions[i]
            writeColumnHeader(dimension, i)
        }

        writer.endArray()
    }

    protected void writeRowHeaders(DataTableRow row) {
        writer.name('rowHeaders')
        writer.beginArray()
        for (int i=0; i<table.rowDimensions.size(); i++) {
            def rowDim = table.rowDimensions[i]
            def element = row.elements[i]
            writer.beginObject()
            writer.name('dimension').value(rowDim.name)
            if (rowDim.elementsSerializable) {
                writer.name('element')
                writeValue(element)
            } else {
                writer.name('key')
                writeValue(rowDim.getKey(element))
            }
            writer.endObject()
        }
        writer.endArray()
    }

    protected void writeRow(DataTableRow row) {
        writer.beginObject()
        writeRowHeaders(row)

        writer.name('cells')
        writer.beginArray()
        def cells = ((Table<DataTableRow, DataTableColumn, Collection<HypercubeValue>> /*work around compiler bug*/)
                        table).row(row)
        for (def column: table.columnKeys) {
            def values = cells[column]
            if (values == null || values.size() == 0) {
                writer.nullValue()
            } else if(values.size() == 1) {
                writeValue(values[0].value)
            } else {
                writer.beginArray()
                for(def hv : values) {
                    writeValue(hv?.value)
                }
                writer.endArray()
            }
        }
        writer.endArray()

        writer.endObject()
    }

    protected void writeRows() {
        writer.name('rows')
        writer.beginArray()
        for (DataTableRow row : table.rowKeys) {
            writeRow(row)
        }
        writer.endArray()
    }

    protected void writeDimension(Dimension dimension) {
        writer.beginObject()
        writer.name('name').value(dimension.name)
        if (!dimension.elementsSerializable) {
            Collection<Object> dimensionKeys
            if (dimension in table.rowDimensions) {
                int i = table.rowDimensions.indexOf(dimension)
                dimensionKeys = table.rowKeys.stream()
                        .map({ DataTableRow row -> dimension.getKey(row.elements[i]) })
                        .distinct()
                        .collect(Collectors.toList())
            } else {
                int i = table.columnDimensions.indexOf(dimension)
                dimensionKeys = table.columnKeys.stream()
                        .map({ DataTableColumn column -> dimension.getKey(column.elements[i]) })
                        .distinct()
                        .collect(Collectors.toList())
            }
            writer.name('elements').beginObject()
            for (def element: dimension.resolveElements(dimensionKeys)) {
                def key = dimension.getKey(element)
                writer.name(key.toString())
                Map value = (Map) dimension.asSerializable(element)
                value.label = key
                writeValue(value)
            }
            writer.endObject()
        }
        writer.endObject()
    }

    protected void writeDimensions(String type, List<Dimension> dimensions) {
        writer.name("${type}Dimensions")
        writer.beginArray()
        for (def dimension : dimensions) {
            writeDimension(dimension)
        }
        writer.endArray()
    }

    protected void writeSorting() {
        writer.name('sort').beginArray()
        for (def entry : table.sort) {
            writer.beginObject()
            writer.name('dimension').value(entry.key.name)
            writer.name('sortOrder').value(entry.value.string())
            if(entry.key in table.requestedSort) {
                writer.name('userRequested').value(true)
            }
            writer.endObject()
        }
        writer.endArray()
    }

    protected void writeOtherKeys() {
        writer.name('offset').value(table.offset)
        if (table.totalRowCount != null) {
            writer.name('rowCount').value(table.totalRowCount)
        }
    }

    protected void writeData() {
        writer.beginObject()
        writeColumnHeaders()
        writeRows()
        writeDimensions('row', table.rowDimensions)
        writeDimensions('column', table.columnDimensions)
        writeSorting()
        writeOtherKeys()

        writer.endObject()
        writer.flush()
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
            throw new UnexpectedResultException("Unexpected value of type ${value.class.simpleName}: $value")
        }
    }
}
