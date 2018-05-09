package org.transmartproject.rest.serialization.tabular

import com.opencsv.CSVWriter
import grails.converters.JSON
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.StreamingDataTable

import java.util.zip.ZipEntry

@InheritConstructors
@CompileStatic
class DataTableTSVSerializer extends AbstractTSVSerializer {

    /**
     * Writes a data table to the output stream.
     * Does not close the output stream afterwards.
     *
     * @param dataTable table to write.
     * @param zipOutStream the stream to write to.
     */
    void writeDataTableToZip(StreamingDataTable dataTable) {

        zipOutStream.putNextEntry(new ZipEntry('metadata.json'))
        writeMetaData(dataTable)
        zipOutStream.closeEntry()

        zipOutStream.putNextEntry(new ZipEntry('data.tsv'))
        def rowElements = writeValues(dataTable)
        zipOutStream.closeEntry()

        dataTable.columnDimensions.eachWithIndex { Dimension dim, int i ->
            if(dim.elementsSerializable) return

            zipOutStream.putNextEntry(new ZipEntry("${dim.name}.tsv"))
            writeDimensionElements(dim, dataTable.columnKeys*.elements*.getAt(i) as LinkedHashSet)
            zipOutStream.closeEntry()
        }

        rowElements.indices.each { i ->
            Dimension dim = dataTable.rowDimensions[i]
            def elems = rowElements[i]
            if(elems != null) {
                zipOutStream.putNextEntry(new ZipEntry("${dim.name}.tsv"))
                writeDimensionElements(dim, elems)
                zipOutStream.closeEntry()
            }
        }
    }

    private void writeMetaData(StreamingDataTable dataTable) {
        def writer = new OutputStreamWriter(zipOutStream)
        def description = [
                row_dimensions: dataTable.rowDimensions*.name,
                column_dimensions: dataTable.columnDimensions*.name,
                sort: dataTable.sort.collect { dim, sort ->
                    ((Map) [dimension: dim.name, order: sort.string()]) +
                            (dataTable.requestedSort.containsKey(dim) ? [user_requested: true] : [:])
                }
        ]
        writer.write((description as JSON).toString(true))
        writer.flush()
    }

    private void writeHeaders(CSVWriter csvWriter, StreamingDataTable dataTable) {
        if(!dataTable.columnKeys) return

        for(int i=0; i<dataTable.columnDimensions.size(); i++) {
            def dim = dataTable.columnDimensions[i]
            def headerValues = (List) [''] * dataTable.rowDimensions.size()
            for(def col : dataTable.columnKeys) {
                headerValues.add(dim.getKey(col.elements[i]))
            }
            writeRow(csvWriter, headerValues)
        }
    }

    private List<Set> writeValues(StreamingDataTable dataTable) {
        CSVWriter csvWriter = getCSVWriter()

        writeHeaders(csvWriter, dataTable)

        List<Set> rowElements = dataTable.rowDimensions.collect { it.elementsSerializable ? null : [] as Set }

        for (row in dataTable) {
            for(int i=0; i<rowElements.size(); i++) {
                if(rowElements[i] == null) continue
                rowElements[i].add(row.rowHeader.elements[i])
            }

            List values = row.headerValues
            for(def cells : row.dataValues) {
                def joiner = new StringJoiner(';')
                for(val in cells) { joiner.add(val.toString()) }
                values.add(joiner.toString())
            }
            csvWriter.writeNext(formatRowValues(values))
        }
        csvWriter.flush()

        rowElements
    }

    // Helper to find all keys that apply to a type of object. For map and nested map values, the nested paths that
    // are set are added to the list of names.
    static void findKeys(Set names, List<String> prefix, value) {
        if(value == null) return
        if(!(value instanceof Map)) {
            names.add(prefix)
            return
        }
        def map = (Map) value
        for(entry in map.entrySet()) {
            findKeys(names, prefix + [entry.key.toString()], entry.value)
        }
    }

    private void writeDimensionElements(Dimension dim, Collection elements) {
        def csvWriter = getCSVWriter()

        Set keys = new LinkedHashSet()
        for(def prop : dim.elementFields.values()) {
            if(prop.type in Map) {
                for(def elem : elements) {
                    findKeys(keys, [prop.name], prop.get(elem))
                }
            } else {
                keys.add(prop.name)
            }
        }

        writeRow(csvWriter, ['label'] + keys.collect { it instanceof String ? (String) it : ((Collection) it).join('' + '.') })

        for(def elem : elements) {
            List values = [dim.getKey(elem)]
            for(key in keys) {
                if(key instanceof String) {
                    values.add(dim.elementFields[(String) key].get(elem))
                } else {
                    List<String> path = (List) key
                    values.add(getByPath(path, 1, dim.elementFields[path[0]].get(elem)))
                }
            }
            writeRow(csvWriter, values)
        }

        csvWriter.flush()
    }

    // helper to retrieve an object from a tree of nested maps given a path. If a path does not exist for this
    // object, returns an empty string.
    static def getByPath(List<String> path, int i, object) {
        if(i >= path.size()) {
            if(object == null || object instanceof Map) return ''
            else return object
        } else {
            if (!(object instanceof Map)) return ''
            def map = (Map) object
            return getByPath(path, i+1, map[path[i]])
        }
    }

}
