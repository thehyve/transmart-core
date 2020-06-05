/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest.serialization.tabular

import com.google.common.collect.ImmutableList
import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.util.StopWatch
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.MissingValues
import org.transmartproject.core.ontology.VariableDataType
import org.transmartproject.core.ontology.VariableMetadata
import org.transmartproject.core.users.User
import org.transmartproject.rest.dataExport.WorkingDirectory

import java.text.SimpleDateFormat
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@CompileStatic
class TabularResultSPSSSerializer implements TabularResultSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy")
    private final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

    private final static toSpssLabel(String label) {
        label?.replaceAll(/[^a-zA-Z0-9_.]/, '_')
    }

    static writeSavFile(ImmutableList<DataColumn> columns, File workingDir, File tsvDataFile,
                        ZipOutputStream zipOutStream, String savFileName, String spssDirectoryName) {
        def stopWatch = new StopWatch('Converting to SAV')
        stopWatch.start('Write TSV to zip')
        zipOutStream.putNextEntry(new ZipEntry("${spssDirectoryName}/data.tsv"))
        tsvDataFile.withInputStream { stream ->
            zipOutStream << stream
        }
        zipOutStream.closeEntry()
        stopWatch.stop()

        // Write SPS file to disk and to the outputstream
        stopWatch.start('Write SPS')
        def spsFile = new File(workingDir, 'data.sps')
        spsFile.withOutputStream { outputStream ->
            writeSpsFile(columns, outputStream, tsvDataFile.path, "${savFileName}.sav")
        }
        zipOutStream.putNextEntry(new ZipEntry("${spssDirectoryName}/data.sps"))
        spsFile.withInputStream { stream ->
            zipOutStream << stream
        }
        zipOutStream.closeEntry()
        stopWatch.stop()

        try {
            try {
                def command = 'pspp --version'
                def process = command.execute()
                process.waitForProcessOutput()
                if (process.exitValue() != 0) {
                    log.warn "PSPP not available. Skip saving of ${spssDirectoryName}/${savFileName}.sav."
                    return
                }
            } catch(IOException e) {
                log.warn "PSPP not available. Skip saving of ${spssDirectoryName}/${savFileName}.sav."
                return
            }

            stopWatch.start('Convert to SAV')
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
            def savFile = new File(workingDir, "${savFileName}.sav")
            zipOutStream.putNextEntry(new ZipEntry("${spssDirectoryName}/${savFileName}.sav"))
            savFile.withInputStream { inputStream ->
                zipOutStream << inputStream
            }
            zipOutStream.closeEntry()
            stopWatch.stop()
        } catch(IOException e) {
            stopWatch.stop()
            log.error "PSPP error: ${e.message}", e
            throw new UnexpectedResultException("PSPP error: ${e.message}", e)
        } finally {
            log.info "Conversion to SAV completed.\n${stopWatch.prettyPrint()}"
        }
    }

    static writeHeader(ImmutableList<DataColumn> columns, OutputStream outputStream) {
        ICSVWriter csvWriter = new CSVWriterBuilder(
                new BufferedWriter(
                        new OutputStreamWriter(outputStream, 'utf-8'),
                        // large 32k chars buffer to reduce overhead
                        32*1024))
            .withSeparator(COLUMN_SEPARATOR)
            .withQuoteChar(ICSVWriter.DEFAULT_QUOTE_CHARACTER)
            .build()
        csvWriter.writeNext(columns.collect { toSpssLabel(it.label) } as String[])
        csvWriter.flush()
    }

    void writeValues(ImmutableList<DataColumn> columns, TabularResult tabularResult, OutputStream outputStream) {
        ICSVWriter csvWriter = new CSVWriterBuilder(
                new BufferedWriter(
                        new OutputStreamWriter(outputStream, 'utf-8'),
                        // large 32k chars buffer to reduce overhead
                        32*1024))
            .withSeparator(COLUMN_SEPARATOR)
            .withQuoteChar(ICSVWriter.DEFAULT_QUOTE_CHARACTER)
            .build()
        Iterator<DataRow> rows = tabularResult.rows
        while (rows.hasNext()) {
            DataRow row = rows.next()
            String[] formattedValuesRow = getFormattedValuesRow(columns, row)
            csvWriter.writeNext(formattedValuesRow)
        }
        csvWriter.flush()
    }

    private String[] getFormattedValuesRow(ImmutableList<DataColumn> columns, DataRow row) {
        columns.stream().map({ DataColumn column ->
            getFormattedValue(column, row)
        }).collect(Collectors.toList()).toArray(new String[0])
    }

    private String getFormattedValue(DataColumn column, DataRow row) {
        VariableMetadata metadata = getColumnMetadata(column)
        def value = row[column]
        return formatRowValue(value, metadata)
    }

    private static VariableMetadata getColumnMetadata(DataColumn column) {
        if (column instanceof MetadataAwareDataColumn && ((MetadataAwareDataColumn)column).metadata) {
            return ((MetadataAwareDataColumn)column).metadata
        } else {
            return null
        }
    }

    private String formatRowValue(Object value, VariableMetadata metadata) {
        if (value == null)
            return ''
        else if (value instanceof Date) {
            return formatDateRowValue((Date)value, metadata)
        } else {
            return toStringWithoutNewLineChar(value)
        }
    }

    private static String toStringWithoutNewLineChar(value) {
        // in pspp the end of a line always separates fields, regardless of DELIMITERS
        // so field with new line char, even surrounded by qualifier, breaks the sav file
        value.toString().replaceAll("\r", "").replaceAll("\n", " ")
    }

    private String formatDateRowValue(Date value, VariableMetadata metadata) {
        if (metadata.type == VariableDataType.DATE) {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.format(value)
            }
        } else {
            synchronized (DATETIME_FORMAT) {
                return DATETIME_FORMAT.format(value)
            }
        }
    }

    static writeSpsFile(List<DataColumn> columnList,
                        OutputStream outputStream,
                        String dataFile,
                        String outputFile = null) {
        if (!columnList) {
            throw new IllegalArgumentException("Can't write sps expression file for empty table.")
        }
        def columns = columnList.findAll { it instanceof MetadataAwareDataColumn && it.metadata } as List<MetadataAwareDataColumn>
        if (columns.size() < columnList.size()) {
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
                '/QUALIFIER = \'"\'',
                '/ARRANGEMENT = DELIMITED',
                '/FIRSTCASE = 2',
                '/VARIABLES =',
                columns.collect { [toSpssLabel(it.label), getSpsDataTypeCode(it.metadata)].join(' ') }.join('\n')
        ].join('\n')
        buffer << '\n.\n'

        List<MetadataAwareDataColumn> columnsWithDescriptions = columns.findAll { it.metadata.description }
        if (columnsWithDescriptions) {
            buffer << 'VARIABLE LABELS\n'
            buffer << columnsWithDescriptions.collect { [toSpssLabel(it.label), quote(it.metadata.description)].join(' ') }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithLabels = columns.findAll { it.metadata.valueLabels }
        if (columnsWithLabels) {
            buffer << 'VALUE LABELS\n'
            buffer << columns
                    .findAll { it.metadata.valueLabels }
                    .collect { column ->
                (([toSpssLabel(column.label)] as List<String>)
                        + column.metadata.valueLabels
                        .collect { value, label -> quote(value as String) + ' ' + quote(label) }).join('\n')
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithMissingValues = columns.findAll { it.metadata.missingValues }
        if (columnsWithMissingValues) {
            buffer << 'MISSING VALUES\n'
            buffer << columnsWithMissingValues.collect { column ->
                "${toSpssLabel(column.label)} ${missingValueExpression(column.metadata.type, column.metadata.missingValues)}"
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithMeasures = columns.findAll { it.metadata.measure }
        if (columnsWithMeasures) {
            buffer << 'VARIABLE LEVEL\n'
            buffer << columnsWithMeasures.collect { column ->
                "${toSpssLabel(column.label)} (${column.metadata.measure})"
            }.join('\n/')
            buffer << '\n.\n'
        }

        List<MetadataAwareDataColumn> columnsWithColumns = columns.findAll { it.metadata.columns }
        if (columnsWithColumns) {
            buffer << 'VARIABLE WIDTH\n'
            buffer << columnsWithColumns.collect { column ->
                "${toSpssLabel(column.label)} (${column.metadata.columns})"
            }.join('\n/')
            buffer << '\n.\n'
        }

        if (outputFile) {
            buffer << "SAVE /OUTFILE=\"${outputFile}\"\n/COMPRESSED.\n"
        }

        buffer << 'EXECUTE.'
        outputStream << buffer
    }

    private static String missingValueExpression(VariableDataType type, MissingValues missingValues) {
        List<String> parts = []
        if (missingValues.lower || missingValues.upper) {
            parts.add((missingValues.lower as String ?: 'LOWEST') + ' THRU ' + (missingValues.upper as String ?: 'HIGHEST'))
        }
        if (missingValues.values) {
            def valuesText = missingValues.values.collect({ value ->
                if (type == VariableDataType.STRING) {
                    quote(value as String)
                } else {
                    value as String
                }
            }).join(', ')
            parts.add(valuesText)
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
        def width = metadata.width
        switch (type) {
            case VariableDataType.NUMERIC:
                return 'F' + (width ?: '') + (metadata.decimals ? '.' + metadata.decimals : '')
            case VariableDataType.DATE:
            case VariableDataType.DATETIME:
                def typeName = type.name()
                if (!width || width < 10 || width > 40) {
                    log.warn "Invalid width for ${typeName} type: ${width}."
                    width = 22
                }
                return typeName + (width ?: '')
            case VariableDataType.STRING:
                return 'A' + (width ?: '255')
            default: throw new UnsupportedOperationException()
        }
    }


    final User user
    final ZipOutputStream zipOutputStream
    final File workingDir
    final String fileName
    final String spssDirectoryName
    final ImmutableList<DataColumn> columns
    final SortedMap<Integer, File> dataFiles = Collections.synchronizedSortedMap([:] as TreeMap)
    final SortedMap<Integer, File> errorFiles = Collections.synchronizedSortedMap([:] as TreeMap)

    /**
     * Initialises the serializer with the output stream.
     * Does not close the output stream afterwards.
     *
     * @param user the user to serialize for. The user specific working directory
     * will be used for temporary files.
     * @param zipOutStream the stream to write to.
     */
    TabularResultSPSSSerializer(User user, ZipOutputStream zipOutputStream, ImmutableList<DataColumn> columns,
                                String fileName) {
        this.user = user
        this.zipOutputStream = zipOutputStream
        if (!columns) {
            throw new IllegalArgumentException("Can't write spss files for empty table.")
        }
        this.columns = columns
        this.workingDir = WorkingDirectory.createDirectoryUser(user, 'transmart-sav-', '-tmpdir')
        this.fileName = fileName
        this.spssDirectoryName = "${fileName}_spss"
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"))
        DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"))
    }

    @Override
    void writeParallel(TabularResult tabularResult, int task) {
        if (!tabularResult.indicesList) {
            throw new IllegalArgumentException("Can't write spss files for empty table.")
        }

        def taskUuid = UUID.randomUUID().toString()
        try {
            // Write TSV file to disk
            def t1 = new Date()
            def tsvDataFile = new File(workingDir, "data-${taskUuid}.tsv")
            tsvDataFile.withOutputStream { outputStream ->
                writeValues(columns, tabularResult, outputStream)
            }
            dataFiles[task] = tsvDataFile
            def t2 = new Date()
            log.info "Export task ${task} [${taskUuid}] completed in ${t2.time - t1.time} ms."
        } catch(Exception e) {
            log.error "Error in task ${task}: ${e.message}", e
            def errorFile = new File(workingDir, "error-${taskUuid}.tsv")
            errorFile.withOutputStream { outputStream ->
                outputStream << e.message
            }
            errorFiles[task] = errorFile
        }
    }

    @Override
    void combine() {
        log.info 'Combining parallel results to a single data file ...'
        try {
            def tsvDataFile = new File(workingDir, 'data.tsv')
            tsvDataFile.withOutputStream { outputStream ->
                writeHeader(columns, outputStream)
                for (File dataFile: dataFiles.values()) {
                    dataFile.withInputStream { inputStream ->
                        outputStream << inputStream
                    }
                }
            }
            writeSavFile(columns, workingDir, tsvDataFile, zipOutputStream, fileName, spssDirectoryName)
        } catch(Exception e) {
            zipOutputStream.putNextEntry(new ZipEntry("${spssDirectoryName}/${fileName}.sav.err"))
            zipOutputStream << e.message
            zipOutputStream.closeEntry()
            for (File errorFile: errorFiles.values()) {
                zipOutputStream.putNextEntry(new ZipEntry("${spssDirectoryName}/${errorFile.name}"))
                errorFile.withInputStream { inputStream ->
                    zipOutputStream << inputStream
                }
                zipOutputStream.closeEntry()
            }
        } finally {
            log.info 'Writing to SPSS completed.'
            workingDir.delete()
        }
    }

}
