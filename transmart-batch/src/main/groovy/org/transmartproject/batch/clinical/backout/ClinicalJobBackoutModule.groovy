package org.transmartproject.batch.clinical.backout

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.listener.ExecutionContextPromotionListener
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.backout.BackoutContext
import org.transmartproject.batch.backout.BackoutModule
import org.transmartproject.batch.batchartifacts.FoundExitStatusChangeListener
import org.transmartproject.batch.batchartifacts.LogCountsStepListener

import javax.annotation.Resource

/**
 * Backout logic for clinical data, except patients.
 */
class ClinicalJobBackoutModule extends BackoutModule {

    @Autowired
    DetectClinicalDataTasklet detectClinicalDataTasklet

    @Resource
    Step deleteConceptsAndFactsStep

    @Autowired
    StepBuilderFactory steps

    @Override
    Step detectPresenceStep() {
        steps.get('clinical.presence')
                .tasklet(detectClinicalDataTasklet)
                .listener(new LogCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .listener(new ExecutionContextPromotionListener(
                        keys: [BackoutContext.KEY_CONCEPTS_TO_DELETE] as String[],
                        strict: true))
                .build()
    }

    @Override
    Step deleteDataStep() {
        deleteConceptsAndFactsStep
    }
}
