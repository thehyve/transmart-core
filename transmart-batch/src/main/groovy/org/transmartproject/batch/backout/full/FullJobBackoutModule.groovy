package org.transmartproject.batch.backout.full

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.listener.ExecutionContextPromotionListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.transmartproject.batch.backout.BackoutContext
import org.transmartproject.batch.backout.BackoutModule
import org.transmartproject.batch.batchartifacts.*
import org.transmartproject.batch.beans.OverriddenNameStep
import org.transmartproject.batch.highdim.assays.BackoutAssayIdReader
import org.transmartproject.batch.patient.GatherCurrentPatientsReader

import javax.annotation.Resource

/**
 * Deletes the top node and the patients for a study, backing off
 * if there still exist other concepts or assays.
 *
 * NOTE: this is not a full @Configuration class, so the bean definition
 * possibilities are more constrained. No auto proxy wrapped scoped beans are
 * possible, for instance. Define only singletons.
 */
class FullJobBackoutModule extends BackoutModule {

    public static final int STRAY_ASSAYS_CHUNK_SIZE = 1000
    public static final int DELETE_PATIENTS_CHUNK_SIZE = 1000
    public static final int FULL_MODULE_PRECEDENCE = BackoutModule.DEFAULT_PRECEDENCE + 100

    @Autowired
    ApplicationContext applicationContext

    @Resource
    Step deleteConceptsAndFactsStep

    @Autowired
    StepBuilderFactory steps

    int order = FULL_MODULE_PRECEDENCE

    @Override
    Step detectPresenceStep() {
        applicationContext.getBean('full.detectPresenceStep', Step)
    }

    @Bean(name = 'full.detectPresenceStep')
    Step detectPresenceStep(CountExistingConceptsTasklet countExistingConceptsTasklet,
                            FindTopNodeTasklet findTopNodeTasklet,
                            BackoutAssayIdReader backoutAssayIdReader) {
        def existingConceptsStep = steps.get('existingConcepts')
                .tasklet(countExistingConceptsTasklet)
                .listener(new LogCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()
        def failOnConceptsFound = steps.get('failOnConceptsFound')
                .tasklet(new FailWithMessageTasklet(
                        'Failing job due to concepts detected on the last step'))
                .build()

        /* in principle, we should not have stray assays here, because
         * any assays should be linked to a concept, and we already
         * confirmed we have no stray concepts
         */
        Step strayAssaysStep = steps.get('strayAssaysStep')
                .chunk(STRAY_ASSAYS_CHUNK_SIZE)
                .reader(backoutAssayIdReader)
                .writer(new NullWriter())
                .listener(new LogCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()
        def failOnAssaysFound = steps.get('failOnObjectsFound')
                .tasklet(new FailWithMessageTasklet(
                        'Failing job due to assays associated with this ' +
                                'study being found in the previous step'))
                .build()

        Step findTopNodeStep = steps.get('findTopNode')
                .tasklet(findTopNodeTasklet)
                .listener(new LogCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .listener(new ExecutionContextPromotionListener(
                        keys: [BackoutContext.KEY_CONCEPTS_TO_DELETE] as String[],
                        strict: true))
                .build()

        SimpleFlow flow = new FlowBuilder<SimpleFlow>('full.presence.flow')
                .start(existingConceptsStep)
                .on('FOUND').to(failOnConceptsFound)
                .from(existingConceptsStep)
                .next(strayAssaysStep)
                .on('FOUND').to(failOnAssaysFound)
                .from(strayAssaysStep)
                .next(findTopNodeStep)
                .on('FOUND').end('FOUND') // yep, needed
                .build()

        steps.get('full.presence')
            .flow(flow)
            .build()
    }

    @Override
    Step deleteDataStep() {
        applicationContext.getBean('full.deleteDataStep', Step)
    }

    @Bean(name = 'full.deleteDataStep')
    Step deleteDataStep(GatherCurrentPatientsReader gatherCurrentPatientsReader,
                        DeletePatientsWriter deletePatientsWriter) {
        Step deletePatientsStep = steps.get('deletePatients')
                .chunk(DELETE_PATIENTS_CHUNK_SIZE)
                .reader(gatherCurrentPatientsReader)
                .writer(deletePatientsWriter)
                .listener(new LogCountsStepListener())
                .build()

        Step deleteConceptsAndFactsStep = new OverriddenNameStep(
                step: this.deleteConceptsAndFactsStep,
                newName: 'full.deleteConceptsAndFacts',)
        SimpleFlow flow = new FlowBuilder<SimpleFlow>('full.deleteData.flow')
                // deleteSecureObjectStep(null, null) works only in @Configuration
                .start(applicationContext.getBean('deleteSecureObjectsStep', Flow))
                .next(deletePatientsStep)
                .next(deleteConceptsAndFactsStep)
                .build()

        steps.get('full.deleteData')
                .flow(flow)
                .build()
    }

    @Bean(name = 'deleteSecureObjectsStep')
    Flow deleteSecureObjectStep(DeletePermissionsTasklet deletePermissionsTasklet,
                                DeleteSecureObjectTasklet deleteSecureObjectTasklet) {
        Step deletePermissionsStep = steps.get('deletePermissions')
                .tasklet(deletePermissionsTasklet)
                .listener(new LogCountsStepListener())
                .build()

        Step deleteSecureObjectStep = steps.get('deleteSecureObject')
                .tasklet(deleteSecureObjectTasklet)
                .listener(new LogCountsStepListener())
                .build()

        new FlowBuilder<SimpleFlow>(
                'full.deleteData.deletePermissions')
                .start(new StringTemplateBasedDecider(
                        '${params.SECURITY_REQUIRED != "Y" ? "SKIP" : "CONTINUE"}'))
                .on('SKIP').end('COMPLETED')
                .on('CONTINUE')
                .to(deletePermissionsStep)
                .next(deleteSecureObjectStep)
                .build()
    }
}
