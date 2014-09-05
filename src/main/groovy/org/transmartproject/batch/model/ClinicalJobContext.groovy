package org.transmartproject.batch.model

import org.springframework.batch.core.scope.context.ChunkContext
import org.transmartproject.batch.Keys

/**
 *
 */
class ClinicalJobContext implements Serializable {

    File columnMapFile
    File wordMapFile
    List<ColumnMapping> columnMappings
    List<WordMapping> wordMappings
    Map<File,List<String>> dataFileColumnsMap

    static ClinicalJobContext get(ChunkContext ctx) {
        ctx.stepContext.stepExecution.jobExecution.executionContext.get(Keys.CLINICAL_JOB_CONTEXT)
    }

}
