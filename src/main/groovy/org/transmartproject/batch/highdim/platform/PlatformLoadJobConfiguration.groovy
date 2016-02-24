package org.transmartproject.batch.highdim.platform

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.transmartproject.batch.batchartifacts.FoundExitStatusChangeListener
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Beans common to jobs that load annotations.
 *
 * By scanning org.transmartproject.batch.highdim.platform it will provide
 * the following extra beans:
 *
 * - deleteGplInfoTasklet
 * - insertGplInfoTasklet
 * - platformCheckTasklet
 * - platformDataCheckTasklet
 *
 * The following beans must be provided:
 * - mainStep
 * - deleteAnnotationStep
 */
@ComponentScan('org.transmartproject.batch.highdim.platform')
abstract class PlatformLoadJobConfiguration extends AbstractJobConfiguration {

    abstract Step mainStep()

    abstract Step deleteAnnotationsStep()

    @Bean
    Flow mainFlow(Tasklet insertGplInfoTasklet) {

        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(checkPlatformExists(null))

        // if found we have an extra flow
                .on('FOUND')
                .to(removePlatformMaybeFlow(null, null))
                .next(stepOf('insertGplInfoTasklet', insertGplInfoTasklet))

                .from(checkPlatformExists(null))
                .next(stepOf('insertGplInfoTasklet', insertGplInfoTasklet))

                .next(mainStep()) // provided by subclass
                .build()
    }

    @Bean
    @JobScope
    Platform platformObject(@Value('#{jobParameters}') Map<String, Object> parameters) {
        new Platform(
                id: parameters[AbstractPlatformJobSpecification.PLATFORM],
                title: parameters[AbstractPlatformJobSpecification.TITLE],
                organism: parameters[AbstractPlatformJobSpecification.ORGANISM],
                markerType: parameters[AbstractPlatformJobSpecification.MARKER_TYPE],
                genomeRelease: parameters[AbstractPlatformJobSpecification.GENOME_RELEASE],
        )
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource annotationsFileResource() {
        new JobParameterFileResource(parameter: AbstractPlatformJobSpecification.ANNOTATIONS_FILE)
    }

    @Bean
    Step checkPlatformExists(Tasklet platformCheckTasklet) {
        steps.get('checkPlatformExists')
                .tasklet(platformCheckTasklet)
                .listener(logCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()
    }

    @Bean
    Flow removePlatformMaybeFlow(Tasklet platformDataCheckTasklet,
                                 Tasklet deleteGplInfoTasklet) {
        Step platformDataCheckStep = steps.get('checkPlatformDataExists')
                .tasklet(platformDataCheckTasklet)
                .listener(logCountsStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()

        new FlowBuilder<Flow>('removePlatform')
                .start(platformDataCheckStep)
                .on('FOUND').fail()
                .next(deleteAnnotationsStep()) // provided by subclass
                .next(stepOf('deleteGplInfoTasklet', deleteGplInfoTasklet))
                .build()
    }
}
