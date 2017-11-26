/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest.serialization.tabular

import com.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.*
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.users.User
import org.transmartproject.rest.dataExport.WorkingDirectory

import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
class TabularResultSPSSSerializer implements TabularResultSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy hh:mm")

    @Override
    void writeFilesToZip(User user, TabularResult tabularResult, ZipOutputStream zipOutStream) {
        if (!tabularResult.indicesList) {
            throw new IllegalArgumentException("Can't write spss files for empty table.")
        }
        zipOutStream.putNextEntry(new ZipEntry('spss/data.tsv'))
        writeValues(tabularResult, zipOutStream)
        zipOutStream.closeEntry()

        zipOutStream.putNextEntry(new ZipEntry('spss/data.sps'))
        writeSpsFile(tabularResult, zipOutStream, 'data.tsv')
        zipOutStream.closeEntry()

        writeSavFile(user, tabularResult, zipOutStream)
    }

    static writeSavFile(User user, TabularResult tabularResult, ZipOutputStream zipOutStream) {
        if (!tabularResult.indicesList) {
            throw new IllegalArgumentException("Can't write sav file for empty table.")
        }
        try {
            def command = 'pspp --version'
            def process = command.execute()
            process.waitForProcessOutput()
            if (process.exitValue() != 0) {
                log.warn 'PSPP not available. Skip saving of spss/data.sav.'
                return
            }
        } catch(IOException e) {
            log.warn 'PSPP not available. Skip saving of spss/data.sav.'
            return
        }

        // FIXME: This leaks data to the /tmp dir.
        def workingDir = WorkingDirectory.createDirectoryUser(user, 'transmart-sav-', '-tmpdir')

        def tsvDataFile = new File(workingDir, 'data.tsv')
        tsvDataFile.withOutputStream { outputStream ->
            writeValues(tabularResult, outputStream)
        }

        def spsFile = new File(workingDir, 'data.sps')
        spsFile.withOutputStream { outputStream ->
            writeSpsFile(tabularResult, outputStream, tsvDataFile.path, 'data.sav')
        }

        try {
            def command = 'pspp data.sps'
            log.debug "Running PSPP in ${workingDir} ..."
            def process = command.execute((String[])null, workingDir)
            def outStream = new ByteArrayOutputStream()
            def errStream = new ByteArrayOutputStream()
            process.waitForProcessOutput(errStream, outStream)
            log.debug "ERR: ${errStream}"
            if (process.exitValue() != 0) {
                log.error "PSPP error: ${errStream.toString()}"
                throw new UnexpectedResultException("PSPP error: ${errStream.toString()}")
            }
            log.debug "PSPP completed."
            def savFile = new File(workingDir, 'data.sav')
            zipOutStream.putNextEntry(new ZipEntry('spss/data.sav'))
            savFile.withInputStream { inputStream ->
                zipOutStream << inputStream
            }
            zipOutStream.closeEntry()
        } catch(IOException e) {
            log.error "PSPP error: ${e.message}", e
            throw new UnexpectedResultException("PSPP error: ${e.message}", e)
        } finally {
            workingDir.delete()
        }
    }

    static writeValues(TabularResult tabularResult, OutputStream outputStream) {
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream, 'utf-8'), COLUMN_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
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

    static writeSpsFile(TabularResult<? extends MetadataAwareDataColumn ,? extends DataRow> tabularResult,
                        OutputStream outputStream,
                        String dataFile,
                        String outputFile = null) {
        if (!tabularResult.indicesList) {
            throw new IllegalArgumentException("Can't write sps expression file for empty table.")
        }
        List<MetadataAwareDataColumn> columns = tabularResult.indicesList
                .findAll { it instanceof MetadataAwareDataColumn && it.metadata }
        if (columns.size() < tabularResult.indicesList.size()) {
            throw new IllegalArgumentException("All table columns have to contain metadata.")
        }

        def buffer = new StringBuffer()
        buffer << [
                '* Encoding: UTF-8.',
                '* NOTE: If you have put this file in a different folder from the associated data file, ',
                '* you will have to change the FILE location on the line below to point to the physical ',
                '* location of your data file.',
                'GET DATA  /TYPE = TXT',
                '/FILE = "' + dataFile + '"',
                '/DELCASE = LINE',
                '/DELIMITERS = "\\t"',
                '/ARRANGEMENT = DELIMITED',
                '/FIRSTCASE = 2',
                '/VARIABLES =',
                columns.collect { [it.label, getSpsDataTypeCode(it.metadata)].join(' ') }.join('\n')
        ].join('\n')
        buffer << '\n.\n'

        List<MetadataAwareDataColumn> columnsWithDescriptions = columns.findAll { it.metadata.description }
        if (columnsWithDescriptions) {
            buffer << 'VARIABLE LABELS\n'
            buffer << columnsWithDescriptions.collect { [it.label, quote(it.metadata.description)].join(' ') }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithLabels = columns.findAll { it.metadata.valueLabels }
        if (columnsWithLabels) {
            buffer << 'VALUE LABELS\n'
            buffer << columns
                    .findAll { it.metadata.valueLabels }
                    .collect { column ->
                ([column.label]
                        + column.metadata.valueLabels
                        .collect { value, label -> quote(value as String) + ' ' + quote(label) }).join('\n')
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithMissingValues = columns.findAll { it.metadata.missingValues }
        if (columnsWithMissingValues) {
            buffer << 'MISSING VALUES\n'
            buffer << columnsWithMissingValues.collect { column ->
                "${column.label} ${missingValueExpression(column.metadata.missingValues)}"
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithMeasures = columns.findAll { it.metadata.measure }
        if (columnsWithMeasures) {
            buffer << 'VARIABLE LEVEL\n'
            buffer << columnsWithMeasures.collect { column ->
                "${column.label} (${column.metadata.measure})"
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithColumns = columns.findAll { it.metadata.columns }
        if (columnsWithColumns) {
            buffer << 'VARIABLE WIDTH\n'
            buffer << columnsWithColumns.collect { column ->
                "${column.label} (${column.metadata.columns})"
            }.join('\n/')
            buffer << '\n.\n'
        }

        if (outputFile) {
            buffer << "SAVE /OUTFILE=\"${outputFile}\"\n/COMPRESSED.\n"
        }

        buffer << 'EXECUTE.'
        outputStream << buffer
    }

    private static String missingValueExpression(MissingValues missingValues) {
        List<String> parts = []
        if (missingValues.lower || missingValues.upper) {
            parts.add((missingValues.lower ?: 'LOWEST') + ' THRU ' +(missingValues.upper ?: 'HIGHEST'))
        }
        if (missingValues.values) {
            parts.add(missingValues.values.join(', '))
        }
        "(${parts.join(', ')})"
    }

    private static String quote(String s) {
        "'${escapeQuote(s)}'"
    }

    private static String escapeQuote(String s) {
        s.replaceAll("'", "''")
    }

    private static String getSpsDataTypeCode(VariableMetadata metadata) {
        VariableDataType type = metadata.type
        if (!type) {
            throw new IllegalArgumentException("Variable has to have a type specified.")
        }
        switch (type) {
            case VariableDataType.NUMERIC:
                return 'F' + (metadata.width ?: '') + (metadata.decimals ? '.' + metadata.decimals : '')
            case VariableDataType.DATE:
                return 'DATETIME' + (metadata.width ?: '')
            case VariableDataType.STRING:
                return 'A' + (metadata.width ?: '')
            default: throw new UnsupportedOperationException()
        }
    }
}
