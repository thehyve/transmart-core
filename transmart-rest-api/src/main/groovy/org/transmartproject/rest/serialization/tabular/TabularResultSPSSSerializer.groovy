/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest.serialization.tabular

import com.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.VariableDataType
import org.transmartproject.core.dataquery.VariableMetadata
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
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
        zipOutStream.putNextEntry(new ZipEntry('spss/data.tsv'))
        writeValues(tabularResult, zipOutStream)
        zipOutStream.closeEntry()

        zipOutStream.putNextEntry(new ZipEntry('spss/data.sps'))
        writeSpsFile(tabularResult, zipOutStream, 'data.tsv')
        zipOutStream.closeEntry()

        writeSavFile(user, tabularResult, zipOutStream)
    }

    static writeSavFile(User user, TabularResult tabularResult, ZipOutputStream zipOutStream) {
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

    static writeSpsFile(TabularResult tabularResult, OutputStream outputStream, String dataFile, String outputFile = null) {
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
                '/DELCASE = LINE /DELIMITERS = "\\t" /ARRANGEMENT = DELIMITED /FIRSTCASE = 2 /VARIABLES =',
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

        if (outputFile) {
            buffer << "SAVE /OUTFILE=\"${outputFile}\"\n/COMPRESSED.\n"
        }

        buffer << 'EXECUTE.'
        outputStream << buffer
    }

    private static String getSpsDataTypeCode(VariableMetadata metadata) {
        switch (metadata?.type) {
            case VariableDataType.NUMERIC:
                return 'F' + (metadata.width ?: '') + (metadata.decimals ? '.' + metadata.decimals : '')
            case VariableDataType.DATE:
                return 'DATETIME' + (metadata.width ?: '')
            case VariableDataType.STRING:
                return 'A' + (metadata.width ?: '')
            default: return ''
        }
    }
}
