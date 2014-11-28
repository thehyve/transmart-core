package org.transmartproject.batch.clinical.facts

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.LineTokenizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.support.GenericRowReader

import java.nio.file.Files
import java.nio.file.Path

/**
 * Reader of TSV files with clinical data, producing ClinicalDataRow elements
 */
class ClinicalDataRowReader extends GenericRowReader<ClinicalDataRow> {

    @Value("#{jobParameters['COLUMN_MAP_FILE']}")
    Path columnMapFile

    @Value("#{clinicalJobContext.variables}")
    List<ClinicalVariable> variables

    private final LineTokenizer tokenizer = new DelimitedLineTokenizer(
            delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
            quoteCharacter: DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER)

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
    ClinicalDataRow mapLine(String line, int lineNumber) throws Exception {
        ClinicalDataRow row
        if (line) {
            row = new ClinicalDataRow(
                    filename: currentResource.filename,
                    index: lineNumber,
                    values: Arrays.asList(tokenizer.tokenize(line).values),
            )
        }
        row
    }
}
