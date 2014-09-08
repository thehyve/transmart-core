package org.transmartproject.batch.clinical

import org.transmartproject.batch.model.ColumnMapping
import org.transmartproject.batch.model.WordMapping

/**
 *
 */
interface ClinicalJobContext {

    Map getJobParameters()

    List<ColumnMapping> getVariables()

    List<WordMapping> getWordMappings()

}

