package org.transmartproject.rest.serialization.tabular

import au.com.bytecode.opencsv.CSVWriter
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.db.multidimquery.MetadataAwareDataColumn

import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TabularResultTSVSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy hh:mm")

    /**
     * Writes a tabular file content to the csv/tsv file to the output stream.
     * Does not close the output stream afterwards.
     * @param out the stream to write to.
     */
    static writeFilesToZip(TabularResult tabularResult, ZipOutputStream zipOutStream) {
        zipOutStream.putNextEntry(new ZipEntry('data.tsv'))
        writeValues(tabularResult, zipOutStream)
        zipOutStream.closeEntry()

        zipOutStream.putNextEntry(new ZipEntry('variables.tsv'))
        writeColumnsMetadata(tabularResult, zipOutStream)
        zipOutStream.closeEntry()

        if (tabularResult.indicesList
                .any { it instanceof MetadataAwareDataColumn && it.metadata?.valueLabels }) {
            zipOutStream.putNextEntry(new ZipEntry('value_labels.tsv'))
            writeColumnsValueMappings(tabularResult, zipOutStream)
            zipOutStream.closeEntry()
        }
    }

    static writeValues(TabularResult tabularResult, OutputStream outputStream) {
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), COLUMN_SEPARATOR)
        List<DataColumn> columns = tabularResult.indicesList
        csvWriter.writeNext(columns*.label as String[])
        tabularResult.rows.each { DataRow row ->
            List valuesRow = columns.collect { DataColumn column -> row[column] }
            csvWriter.writeNext(formatRowValues(valuesRow))
        }
        csvWriter.flush()
    }

    private static String[] formatRowValues(List<Object> valuesRow) {
        valuesRow.collect { value ->
            if (value instanceof Date) {
                DATE_FORMAT.format(value)
            } else {
                value as String
            }
        } as String[]
    }

    static writeColumnsMetadata(TabularResult tabularResult, OutputStream outputStream) {
        List<MetadataAwareDataColumn> columns = tabularResult.indicesList
                .findAll { it instanceof MetadataAwareDataColumn }
        if (!columns) return
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), COLUMN_SEPARATOR)
        csvWriter.writeNext(['name', 'type', 'width', 'decimals', 'description', 'columns', 'measure'] as String[])
        columns.each { MetadataAwareDataColumn column ->
            csvWriter.writeNext([
                    column.label,
                    column.metadata.type,
                    column.metadata.width,
                    column.metadata.decimals,
                    column.metadata.description,
                    column.metadata.columns,
                    column.metadata.measure,
            ] as String[])
        }
        csvWriter.flush()
    }

    static writeColumnsValueMappings(TabularResult tabularResult, OutputStream outputStream) {
        List<MetadataAwareDataColumn> columns = tabularResult.indicesList
                .findAll { it instanceof MetadataAwareDataColumn && it.metadata?.valueLabels }
        if (!columns) return
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), COLUMN_SEPARATOR)
        csvWriter.writeNext(['name', 'value', 'label'] as String[])
        columns.each { MetadataAwareDataColumn column ->
            column.metadata.valueLabels.each { value, label ->
                csvWriter.writeNext([
                        column.label,
                        value,
                        label,
                ] as String[])
            }
        }
        csvWriter.flush()
    }

}
