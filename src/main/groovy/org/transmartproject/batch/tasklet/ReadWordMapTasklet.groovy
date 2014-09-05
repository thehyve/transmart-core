package org.transmartproject.batch.tasklet

import org.springframework.batch.core.scope.context.ChunkContext
import org.transmartproject.batch.model.ClinicalJobContext
import org.transmartproject.batch.model.WordMapping

/**
 *
 */
class ReadWordMapTasklet extends ReadFileTasklet<WordMapping> {

    ReadWordMapTasklet() {
        reader = WordMapping.READER
    }

    @Override
    File getInputFile(ChunkContext ctx) {
        return ClinicalJobContext.get(ctx).wordMapFile
    }

    @Override
    void setResult(ChunkContext ctx, List<WordMapping> result) {

        result.each {
            if (it.newValue == 'null') {
                it.newValue = null //we want the value null, not the string 'null'
            }
        }

        ClinicalJobContext.get(ctx).wordMappings = result
    }
}
