package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.Variable

/**
 *
 */
class InsertUpdatePatientsTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext jobContext

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        /*
        jobContext.patientSet.patientMap.each {
            println "$it.key: ${toString(it.value.demographicRelatedValues)}"
        }
        */

        Set<Patient> all = jobContext.patientSet.patientMap.values()
        int count = all.count { it.persisted }
        println "$count patients to update"
        println "${all.size() - count} patients to insert"

        return RepeatStatus.FINISHED
    }

    String toString(Map<Variable, Object> values) {
        values.collectEntries { [(it.key.demographicVariable.column),it.value ] }.toString()
    }
}
