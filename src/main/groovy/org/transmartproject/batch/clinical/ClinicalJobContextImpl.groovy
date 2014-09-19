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
import org.transmartproject.batch.support.Keys

import javax.annotation.PostConstruct

@Scope('job')
@Component('clinicalJobContext')
class ClinicalJobContextImpl implements ClinicalJobContext {

    @Value('#{jobParameters}')
    Map jobParameters

    ConceptTree conceptTree

    PatientSet patientSet

    @PostConstruct
    void init() {
        jobExecutionContext.put(Keys.VARIABLES, new ArrayList())
        jobExecutionContext.put(Keys.WORD_MAPPINGS, new ArrayList())

        String topNode = jobParameters.get(Keys.TOP_NODE)
        if (!topNode) {
            topNode = ConceptTree.getDefaultTopNode(jobParameters.get(Keys.STUDY_ID))
        }

        conceptTree = new ConceptTree(topNode)
        patientSet = new PatientSet()
    }

    //@Value('#{jobExecutionContext}') //cannot inject this as value, as we want the real context, not the immutable Map
    ExecutionContext getJobExecutionContext() {
        JobSynchronizationManager.context.jobExecution.executionContext
    }

    List<Variable> getVariables() {
        jobExecutionContext.get(Keys.VARIABLES)
    }

    List<WordMapping> getWordMappings() {
        jobExecutionContext.get(Keys.WORD_MAPPINGS)
    }

}
