package org.transmartproject.batch.highdim.platform

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.FailWithMessageTasklet
import org.transmartproject.batch.batchartifacts.FoundExitStatusChangeListener
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Patient spring batch steps configuration
 */
@Configuration
@ComponentScan
class PlatformStepsConfig implements StepBuildingConfigurationTrait {

    @Bean
    Step checkPlatformNotFound(Tasklet platformCheckTasklet) {
        steps.get('checkPlatformNotFound')
                .allowStartIfComplete(true)
                .tasklet(platformCheckTasklet)
                .listener(new FoundExitStatusChangeListener(notifyOnFound: false))
                .build()
    }

    @Bean
    Step checkPlatformFound(Tasklet platformCheckTasklet) {
        steps.get('checkPlatformFound')
                .allowStartIfComplete(true)
                .tasklet(platformCheckTasklet)
                .listener(new FoundExitStatusChangeListener(notifyOnFound: true))
                .build()
    }

    @Bean
    Step failWithPlatformNotFoundMessage() {
        stepOf('platformNotFound',
                new FailWithMessageTasklet(
                        "Load platform \${ctx['$PlatformJobContextKeys.PLATFORM']} before"))
    }

    @Bean
    Step ensureThePlatformExists(Step checkPlatformNotFound, Step failWithPlatformNotFoundMessage) {
        //TODO The flow condition look cumbersome. We should simplify this
        SimpleFlow ensureThePlatformExistsFlow = new FlowBuilder<SimpleFlow>('ensureThePlatformExistsFlow')
                .start(checkPlatformNotFound)
                .on('NOT FOUND').to(failWithPlatformNotFoundMessage)
                .from(checkPlatformNotFound).on('*').end()
                .build()

        steps.get('ensureThePlatformExists')
                .allowStartIfComplete(true)
                .flow(ensureThePlatformExistsFlow)
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
    Step deleteGplInfo(Tasklet deleteGplInfoTasklet) {
        stepOf('deleteGplInfo', deleteGplInfoTasklet)
    }

    @Bean
    Step insertGplInfo(Tasklet insertGplInfoTasklet) {
        stepOf('insertGplInfo', insertGplInfoTasklet)
    }

}
