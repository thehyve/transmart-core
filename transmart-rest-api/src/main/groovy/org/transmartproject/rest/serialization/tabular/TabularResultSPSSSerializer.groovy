package org.transmartproject.rest.serialization.tabular

import au.com.bytecode.opencsv.CSVWriter
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.ColumnDataType
import org.transmartproject.core.dataquery.ColumnMetadata
import org.transmartproject.core.dataquery.MetadataAwareDataColumn

import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TabularResultSPSSSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy hh:mm")

    /**
     * Writes a tabular file content to the output stream.
     * Does not close the output stream afterwards.
     * @param out the stream to write to.
     */
    static writeFilesToZip(TabularResult tabularResult, ZipOutputStream zipOutStream) {
        def tsvDataFile = 'data.tsv'
        zipOutStream.putNextEntry(new ZipEntry(tsvDataFile))
        writeValues(tabularResult, zipOutStream)
        zipOutStream.closeEntry()

        zipOutStream.putNextEntry(new ZipEntry('data.sps'))
        writeSpsFile(tabularResult, zipOutStream, tsvDataFile)
        zipOutStream.closeEntry()
    }

    static writeValues(TabularResult tabularResult, OutputStream outputStream) {
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), COLUMN_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
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
            if (value == null) return ''
            if (value instanceof Date) {
                DATE_FORMAT.format(value)
            } else {
                value as String
            }
        } as String[]
    }

    static writeSpsFile(TabularResult tabularResult, OutputStream outputStream, String dataFile) {
        List<MetadataAwareDataColumn> columns = tabularResult.indicesList
                .findAll { it instanceof MetadataAwareDataColumn }
        if (!columns) return
        def buffer = new StringBuffer()
        buffer << [
                '* Encoding: UTF-8.',
                '* NOTE: If you have put this file in a different folder from the associated data file, ',
                '* you will have to change the FILE location on the line below to point to the physical ',
                '* location of your data file.',
                'GET DATA  /TYPE = TXT/FILE = ',
                '"' + dataFile + '"',
                '/DELCASE = LINE /DELIMITERS = "\\t" /ARRANGEMENT = DELIMITED /FIRSTCASE = 2 /IMPORTCASE = ALL /VARIABLES =',
                ''
        ].join('\n')
        buffer << columns.collect { [it.label, getSpsDataTypeCode(it.metadata)].join(' ') }.join('\n')
        buffer << '\n.\n'

        buffer << 'VARIABLE LABELS\n'
        buffer << columns.collect { [it.label, '"' + it.metadata.description + '"', '/'].join(' ') }.join('\n')
        buffer << '\n.\n'

        List<MetadataAwareDataColumn> columnsWithLabels = columns.findAll { it.metadata.valueLabels }
        if (columnsWithLabels) {
            buffer << 'VALUE LABELS\n'
            buffer << columns
                    .findAll { it.metadata.valueLabels }
                    .collect { column ->
                ([column.label]
                        + column.metadata.valueLabels.collect { value, label -> "'${value}' \"${label}\"" }
                        + '/').join('\n')
            }.join('\n')
            buffer << '\n.\n'
        }

        buffer << 'EXECUTE.'
        outputStream << buffer
    }

    private static String getSpsDataTypeCode(ColumnMetadata metadata) {
        switch (metadata?.type) {
            case ColumnDataType.NUMERIC:
                return 'F' + (metadata.width ?: '') + (metadata.decimals ? '.' + metadata.decimals : '')
            case ColumnDataType.DATE:
                return 'DATETIME' + (metadata.width ?: '')
            case ColumnDataType.STRING:
                return 'A' + (metadata.width ?: '')
            default: return ''
        }
    }
}
