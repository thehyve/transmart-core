package org.transmartproject.batch.clinical

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.facts.WordMapping
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.patient.PatientSet

/**
 * Simplifies access to data needed throughout the job.
 *
 * All the data is kept in memory now. The job context is not used.
 */
@Scope('job')
@Component('clinicalJobContext')
class ClinicalJobContextImpl implements ClinicalJobContext {

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value('#{jobParameters}')
    Map jobParameters

    @Autowired
    ConceptTree conceptTree

    @Autowired
    PatientSet patientSet

    List<ClinicalVariable> variables = []

    List<WordMapping> wordMappings = []
}
