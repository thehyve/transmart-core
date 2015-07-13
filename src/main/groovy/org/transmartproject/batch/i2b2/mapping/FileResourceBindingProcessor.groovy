package org.transmartproject.batch.i2b2.mapping

import groovy.transform.TypeChecked
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

import java.nio.file.Path

/**
 * Sets {@link I2b2MappingEntry#fileResource}.
 */
@Component
@JobScopeInterfaced
@TypeChecked
class FileResourceBindingProcessor implements ItemProcessor<I2b2MappingEntry, I2b2MappingEntry> {

    @Value("#{jobParameters['COLUMN_MAP_FILE']}")
    Path columnMapFile

    @Override
    I2b2MappingEntry process(I2b2MappingEntry item) throws Exception {
        item.fileResource =
                new FileSystemResource(
                        columnMapFile.resolveSibling(item.filename).toFile())
        item
    }
}
