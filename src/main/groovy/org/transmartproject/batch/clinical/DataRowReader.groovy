package org.transmartproject.batch.clinical

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.GenericRowReader
import org.transmartproject.batch.support.MappingHelper

import java.nio.file.Files
import java.nio.file.Path

/**
 * Reader of TSV files with clinical data, producing Row elements
 */
class DataRowReader extends GenericRowReader<Row> {

    @Value("#{jobParameters['COLUMN_MAP_FILE']}")
    Path columnMapFile

    @Value("#{clinicalJobContext.variables}")
    List<Variable> variables

    @Override
    List<Resource> getResourcesToProcess() {
        Set<String> filenames = variables*.filename as Set

        List<Resource> resources = []
        filenames.each {
            Path filePath = columnMapFile.resolveSibling(it)
            if (!Files.isReadable(filePath) || !Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException(
                        "Not a regular and readable file: $filePath")
            }
            resources.add(new FileSystemResource(filePath.toFile()))
        }
        resources
    }

    @Override
    Row mapLine(String line, int lineNumber) throws Exception {
        Row row
        if (line) {
            row = new Row(
                    filename: currentResource.filename,
                    index: lineNumber,
                    values: MappingHelper.parseValues(line)
            )
        }
        row
    }
}
