package org.transmartproject.batch.clinical

import org.springframework.batch.item.ExecutionContext
import org.transmartproject.batch.model.ColumnMapping
import org.transmartproject.batch.model.WordMapping

/**
 *
 */
interface ClinicalJobContext {

    void setJobExecutionContext(ExecutionContext context)

    Map getJobParameters()

    void setColumnMappings(List<ColumnMapping> list)
    List<ColumnMapping> getColumnMappings()

    void setWordMappings(List<WordMapping> list)
    List<WordMapping> getWordMappings()


}

