/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest.serialization.tabular

import com.google.common.collect.ImmutableList
import com.opencsv.CSVWriter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.util.StopWatch
import org.transmartproject.core.dataquery.*
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
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy hh:mm")

    private final static toSpssLabel(String label) {
        label?.replaceAll(/[^a-zA-Z0-9_.]/, '_')
    }

    static writeSavFile(ImmutableList<DataColumn> columns, File workingDir, File tsvDataFile, ZipOutputStream zipOutStream) {
        def stopWatch = new StopWatch('Converting to SAV')
        stopWatch.start('Write TSV to zip')
        zipOutStream.putNextEntry(new ZipEntry('spss/data.tsv'))
        tsvDataFile.withInputStream { stream ->
            zipOutStream << stream
        }
        zipOutStream.closeEntry()
        stopWatch.stop()

        // Write SPS file to disk and to the outputstream
        stopWatch.start('Write SPS')
        def spsFile = new File(workingDir, 'data.sps')
        spsFile.withOutputStream { outputStream ->
            writeSpsFile(columns, outputStream, tsvDataFile.path, 'data.sav')
        }
        zipOutStream.putNextEntry(new ZipEntry('spss/data.sps'))
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
                    log.warn 'PSPP not available. Skip saving of spss/data.sav.'
                    return
                }
            } catch(IOException e) {
                log.warn 'PSPP not available. Skip saving of spss/data.sav.'
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
            def savFile = new File(workingDir, 'data.sav')
            zipOutStream.putNextEntry(new ZipEntry('spss/data.sav'))
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
        CSVWriter csvWriter = new CSVWriter(
                new BufferedWriter(
                        new OutputStreamWriter(outputStream, 'utf-8'),
                        // large 32k chars buffer to reduce overhead
                        32*1024),
                COLUMN_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
        csvWriter.writeNext(columns.collect { toSpssLabel(it.label) } as String[])
        csvWriter.flush()
    }

    void writeValues(ImmutableList<DataColumn> columns, TabularResult tabularResult, OutputStream outputStream) {
        CSVWriter csvWriter = new CSVWriter(
                new BufferedWriter(
                        new OutputStreamWriter(outputStream, 'utf-8'),
                        // large 32k chars buffer to reduce overhead
                        32*1024),
                COLUMN_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
        Iterator<DataRow> rows = tabularResult.rows
        while (rows.hasNext()) {
            DataRow row = rows.next()
            List<Object> valuesRow = columns.stream().map({ DataColumn column -> row[column] }).collect(Collectors.toList())
            csvWriter.writeNext(formatRowValues(valuesRow))
        }
        csvWriter.flush()
    }

    private String[] formatRowValues(List<Object> valuesRow) {
        valuesRow.stream().map({value ->
            if (value == null) return ''
            if (value instanceof Date) {
                synchronized (DATE_FORMAT) {
                    DATE_FORMAT.format(value)
                }
            } else {
                value.toString()
            }
        }).collect(Collectors.toList()).toArray(new String[0])
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
                "${toSpssLabel(column.label)} ${missingValueExpression(column.metadata.missingValues)}"
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

    private static String missingValueExpression(MissingValues missingValues) {
        List<String> parts = []
        if (missingValues.lower || missingValues.upper) {
            parts.add((missingValues.lower as String ?: 'LOWEST') + ' THRU ' + (missingValues.upper as String ?: 'HIGHEST'))
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
                def width = metadata.width
                if (!width || width < 17 || width > 40) {
                    log.warn "Invalid width for DATETIME type: ${width}."
                    width = 22
                }
                return 'DATETIME' + (width ?: '')
            case VariableDataType.STRING:
                return 'A' + (metadata.width ?: '255')
            default: throw new UnsupportedOperationException()
        }
    }


    final User user
    final ZipOutputStream zipOutputStream
    final File workingDir
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
    TabularResultSPSSSerializer(User user, ZipOutputStream zipOutputStream, ImmutableList<DataColumn> columns) {
        this.user = user
        this.zipOutputStream = zipOutputStream
        if (!columns) {
            throw new IllegalArgumentException("Can't write spss files for empty table.")
        }
        this.columns = columns
        this.workingDir = WorkingDirectory.createDirectoryUser(user, 'transmart-sav-', '-tmpdir')
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
            writeSavFile(columns, workingDir, tsvDataFile, zipOutputStream)
        } catch(Exception e) {
            zipOutputStream.putNextEntry(new ZipEntry('spss/data.sav.err'))
            zipOutputStream << e.message
            zipOutputStream.closeEntry()
            for (File errorFile: errorFiles.values()) {
                zipOutputStream.putNextEntry(new ZipEntry("spss/${errorFile.name}"))
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
