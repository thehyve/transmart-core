package org.transmartproject.rest.serialization.tabular

import com.google.common.collect.ImmutableList
import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.users.User
import org.transmartproject.rest.dataExport.WorkingDirectory

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@CompileStatic
class TabularResultTSVSerializer extends AbstractTSVSerializer implements TabularResultSerializer {

    private void writeValues(TabularResult<DataColumn, DataRow> tabularResult, OutputStream outputStream) {
        ICSVWriter csvWriter = new CSVWriterBuilder(new OutputStreamWriter(outputStream))
                .withSeparator(COLUMN_SEPARATOR).build()
        for (DataRow row in tabularResult) {
            List valuesRow = columns.collect { DataColumn column -> row[column] }
            csvWriter.writeNext(formatRowValues(valuesRow))
        }
        csvWriter.flush()
    }

    static void writeHeader(List<DataColumn> columns, OutputStream outputStream) {
        ICSVWriter csvWriter = new CSVWriterBuilder(new OutputStreamWriter(outputStream))
                .withSeparator(COLUMN_SEPARATOR).build()
        csvWriter.writeNext(columns*.label as String[])
        csvWriter.flush()
    }

    static writeColumnsMetadata(ImmutableList<DataColumn> indicesList, OutputStream outputStream) {
        def columns = indicesList
                .findAll { it instanceof MetadataAwareDataColumn } as List<MetadataAwareDataColumn>
        if (!columns) return
        ICSVWriter csvWriter = new CSVWriterBuilder(new OutputStreamWriter(outputStream))
                .withSeparator(COLUMN_SEPARATOR).build()
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

    static writeColumnsValueMappings(ImmutableList<DataColumn> indicesList, OutputStream outputStream) {
        def columns = indicesList
                .findAll { it instanceof MetadataAwareDataColumn && it.metadata?.valueLabels } as List<MetadataAwareDataColumn>
        if (!columns) return
        ICSVWriter csvWriter = new CSVWriterBuilder(new OutputStreamWriter(outputStream))
                .withSeparator(COLUMN_SEPARATOR).build()
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

    final ImmutableList<DataColumn> columns
    final File workingDir
    final SortedMap<Integer, File> dataFiles = Collections.synchronizedSortedMap([:] as TreeMap)

    TabularResultTSVSerializer(User user, ZipOutputStream zipOutStream, ImmutableList<DataColumn> columns) {
        super(user, zipOutStream)
        this.columns = columns
        this.workingDir = WorkingDirectory.createDirectoryUser(user, 'transmart-tsv-', '-tmpdir')
    }

    @Override
    void writeParallel(TabularResult tabularResult, int task) {
        def taskUuid = UUID.randomUUID().toString()
        // Write TSV file to disk
        def t1 = new Date()
        def tsvDataFile = new File(workingDir, "data-${taskUuid}.tsv")
        tsvDataFile.withOutputStream { outputStream ->
            writeValues(tabularResult, outputStream)
        }
        dataFiles[task] = tsvDataFile
        def t2 = new Date()
        log.info "Export task ${task} [${taskUuid}] completed in ${t2.time - t1.time} ms."
    }

    void combine() {
        log.info 'Combining parallel results to a single TSV file ...'
        try {
            zipOutStream.putNextEntry(new ZipEntry('data.tsv'))
            writeHeader(columns, zipOutStream)
            for (File dataFile: dataFiles.values()) {
                dataFile.withInputStream { inputStream ->
                    zipOutStream << inputStream
                }
            }
            zipOutStream.closeEntry()

            zipOutStream.putNextEntry(new ZipEntry('variables.tsv'))
            writeColumnsMetadata(columns, zipOutStream)
            zipOutStream.closeEntry()

            if (columns.any {
                it instanceof MetadataAwareDataColumn && it.metadata?.valueLabels }) {
                zipOutStream.putNextEntry(new ZipEntry('value_labels.tsv'))
                writeColumnsValueMappings(columns, zipOutStream)
                zipOutStream.closeEntry()
            }
        } finally {
            log.info 'Export to TSV completed.'
            workingDir.delete()
        }
    }

}
