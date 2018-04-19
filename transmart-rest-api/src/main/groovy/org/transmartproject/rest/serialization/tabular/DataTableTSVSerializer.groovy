package org.transmartproject.rest.serialization.tabular

import com.google.common.collect.ImmutableList
import com.opencsv.CSVWriter
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.multidimquery.DataTableColumn
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.users.User
import org.transmartproject.rest.dataExport.WorkingDirectory

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

        zipOutStream.putNextEntry(new ZipEntry('data.tsv'))
        def rowElements = writeValues(dataTable)
        zipOutStream.closeEntry()

        dataTable.columnDimensions.eachWithIndex{ Dimension dim, int i ->
            if(dim.elementsSerializable) return

            zipOutStream.putNextEntry(new ZipEntry("${dim.name}.tsv"))
            writeDimensionElements(dim, dataTable.columnKeys*.elements*.getAt(i))
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
        csvWriter.close()

        rowElements
    }

    void findKeys(Set names, List<String> prefix, value) {
        if(! value instanceof Map) {
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
                    findKeys(keys, [prop.name], elem)
                }
            } else {
                keys.add(prop.name)
            }
        }

        writeRow(csvWriter, keys.collect { it instanceof String ? it : ((Collection) it).join('.') })

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

        csvWriter.close()
    }

    def getByPath(List<String> path, int i, object) {
        if(path.size() >= i) {
            if(object == null || object instanceof Map) return ''
            else return object
        }
        if(! object instanceof Map) return ''
        def map = (Map) object
        return getByPath(path, i++, map[path[i]])
    }

}
