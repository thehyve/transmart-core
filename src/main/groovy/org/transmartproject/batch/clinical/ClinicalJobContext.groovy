package org.transmartproject.batch.clinical

import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.model.WordMapping

/**
 *
 */
interface ClinicalJobContext {

    Map getJobParameters()

    List<Variable> getVariables()

    List<WordMapping> getWordMappings()

}

