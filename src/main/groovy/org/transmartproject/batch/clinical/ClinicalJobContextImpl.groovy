package org.transmartproject.batch.clinical

import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.batch.model.ConceptTree
import org.transmartproject.batch.model.PatientSet
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.model.WordMapping

import javax.annotation.PostConstruct

import static org.transmartproject.batch.clinical.ClinicalJobContextKeys.VARIABLES
import static org.transmartproject.batch.clinical.ClinicalJobContextKeys.WORD_MAPPINGS

@Scope('job')
@Component('clinicalJobContext')
class ClinicalJobContextImpl implements ClinicalJobContext {

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobParameters['TOP_NODE']}")
    String topNode

    @Value('#{jobParameters}')
    Map jobParameters

    ConceptTree conceptTree

    PatientSet patientSet

    @PostConstruct
    void init() {
        jobExecutionContext.put(VARIABLES, new ArrayList())
        jobExecutionContext.put(WORD_MAPPINGS, new ArrayList())

        conceptTree = new ConceptTree(topNode)
        patientSet = new PatientSet()
    }

    //@Value('#{jobExecutionContext}') //cannot inject this as value, as we want the real context, not the immutable Map
    ExecutionContext getJobExecutionContext() {
        JobSynchronizationManager.context.jobExecution.executionContext
    }

    List<Variable> getVariables() {
        jobExecutionContext.get(VARIABLES)
    }

    List<WordMapping> getWordMappings() {
        jobExecutionContext.get(WORD_MAPPINGS)
    }

}
