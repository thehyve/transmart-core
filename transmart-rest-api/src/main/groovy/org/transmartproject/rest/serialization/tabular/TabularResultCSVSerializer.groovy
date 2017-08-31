package org.transmartproject.rest.serialization.tabular

import au.com.bytecode.opencsv.CSVWriter
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult

class TabularResultCSVSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char

    /**
     * Writes a tabular file content to the csv/tsv file to the output stream.
     * Does not close the output stream afterwards.
     * @param out the stream to write to.
     */
    static write(TabularResult tabularResult, OutputStream outputStream) {
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), COLUMN_SEPARATOR)
        List<DataColumn> columns = tabularResult.indicesList
        csvWriter.writeNext([ tabularResult.rowsDimensionLabel ] + columns*.label as String[])
        tabularResult.rows.each { DataRow row ->
            List valuesRow = columns.collect { DataColumn column -> row[column] }
            csvWriter.writeNext([ row.label ] + valuesRow as String[])
        }
        csvWriter.flush()
    }

}
