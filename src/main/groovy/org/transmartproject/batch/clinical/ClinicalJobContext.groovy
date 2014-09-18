package org.transmartproject.batch.clinical

import org.transmartproject.batch.model.ConceptTree
import org.transmartproject.batch.model.PatientSet
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.model.WordMapping

/**
 *
 */
interface ClinicalJobContext {

    List<Variable> getVariables()

    List<WordMapping> getWordMappings()

    ConceptTree getConceptTree()

    PatientSet getPatientSet()

}

