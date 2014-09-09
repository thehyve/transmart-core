package org.transmartproject.batch.clinical

import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.support.MappingHelper

/**
 *
 */
class DataRowReader extends FlatFileItemReader<Row> implements LineMapper<Row> {

    @Autowired
    ClinicalJobContext jobContext

    @Value("#{jobParameters['dataLocation']}")
    String dataLocation

    private List<File> dataFiles
    private int currentFileIndex = -1
    private boolean end = false;

    DataRowReader() {
        setLineMapper(this)
    }

    @Override
    Row read() throws Exception, UnexpectedInputException, ParseException {

        if (end) {
            return null
        } else if (!dataFiles) {
            init()
        }

        Row row = super.read()
        if (!row) {
            nextFile()
            if (!end) {
                row = super.read()
            }
        }

        row
    }

    @Override
    Row mapLine(String line, int lineNumber) throws Exception {
        Row row
        if (line) {
            row = new Row(
                    filename: getCurrentFilename(),
                    index: lineNumber,
                    values: MappingHelper.parseValues(line)
            )
        }
        row
    }

    private String getCurrentFilename() {
        dataFiles[currentFileIndex].name
    }

    private void nextFile() {
        currentFileIndex ++
        end = (currentFileIndex >= dataFiles.size())
        if (!end) {
            setResource(new FileSystemResource(dataFiles[currentFileIndex]))
        }
    }

    private void init() {
        if (dataLocation == null) {
            throw new IllegalArgumentException('Data location not defined')
        }

        Set<String> dataFilenames = jobContext.variables.collect { it.filename } as Set
        currentFileIndex = 0
        dataFiles =  dataFilenames.collect { new File(dataLocation, it) }
        dataFiles.each {
            if (!it.exists()) {
                throw new IllegalArgumentException("Data file not found: $it.absolutePath")
            }
        }
    }
}
