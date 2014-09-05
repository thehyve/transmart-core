package org.transmartproject.batch.tasklet

import org.springframework.batch.core.scope.context.ChunkContext
import org.transmartproject.batch.model.ClinicalJobContext
import org.transmartproject.batch.model.ColumnMapping

/**
 *
 */
class ReadColumnMapTasklet extends ReadFileTasklet<ColumnMapping> {

    ReadColumnMapTasklet() {
        reader = ColumnMapping.READER
    }

    @Override
    File getInputFile(ChunkContext ctx) {
        ClinicalJobContext.get(ctx).columnMapFile
    }

    @Override
    void setResult(ChunkContext ctx, List<ColumnMapping> result) {
        File folder = this.file.getParentFile()
        Set<File> dataFiles = ColumnMapping.getDataFiles(folder, result)
        ColumnMapping.validateDataFiles(dataFiles)
        ClinicalJobContext cjc = ClinicalJobContext.get(ctx)
        cjc.columnMappings = result //set the column mappings
        cjc.dataFileColumnsMap = dataFiles.collectEntries { [(it): null] }  //set the data files (no columns yet)
    }

}
