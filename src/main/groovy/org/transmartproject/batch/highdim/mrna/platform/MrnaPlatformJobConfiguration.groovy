package org.transmartproject.batch.highdim.mrna.platform

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.FlowJob
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.highdim.platform.AbstractPlatformJobParameters
import org.transmartproject.batch.highdim.platform.PlatformLoadJobConfiguration
import org.transmartproject.batch.batchartifacts.FoundExitStatusChangeListener
import org.transmartproject.batch.support.JobParameterFileResource
import org.transmartproject.batch.batchartifacts.LineOfErrorDetectionListener
import org.transmartproject.batch.batchartifacts.ProgressWriteListener

import javax.annotation.Resource

/**
 * Spring configuration for the clinical data job.
 */
@Configuration
@ComponentScan([
        'org.transmartproject.batch.highdim.platform',
        'org.transmartproject.batch.highdim.mrna.platform'])
@Import(PlatformLoadJobConfiguration)
class MrnaPlatformJobConfiguration extends AbstractJobConfiguration {

    public static final String JOB_NAME = 'MrnaPlatformLoadJob'

    static int chunkSize = 5000

    @Resource(name='platformCheckTasklet')
    Tasklet platformCheckTasklet

    @Resource(name='insertGplInfoTasklet')
    Tasklet insertGplInfoTasklet

    @Resource(name='platformDataCheckTasklet')
    Tasklet platformDataCheckTasklet

    @Resource(name='deleteMrnaAnnotationTasklet')
    Tasklet deleteMrnaAnnotationTasklet

    @Resource(name='deleteGplInfoTasklet')
    Tasklet deleteGplInfoTasklet

    @Autowired
    MrnaAnnotationRowValidator annotationRowValidator

    @Autowired
    MrnaAnnotationWriter mrnaAnnotationWriter

    @Bean(name = 'MrnaPlatformLoadJob' /* JOB_NAME */)
    @Override
    Job job() {
        FlowJob job =
            jobs.get(JOB_NAME)
                    .start(mainFlow())
                    .end()
                    .build()
        job.jobParametersIncrementer = jobParametersIncrementer
        job
    }

    @Bean
    Flow mainFlow() {
        /* step that reads the annotations file and writes it to the database */
        def mainStep =  steps.get('mainStep')
                .chunk(chunkSize)
                .reader(tsvFileReader(
                        annotationsFileResource(),
                        beanClass: MrnaAnnotationRow,
                        columnNames: ['gplId', 'probeName', 'genes',
                                      'entrezIds', 'organism']))
                .processor(new ValidatingItemProcessor(
                        adaptValidator(annotationRowValidator)))
                .writer(mrnaAnnotationWriter)
                .listener(new LineOfErrorDetectionListener())
                .listener(new ProgressWriteListener())
                .build()

        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(checkPlatformExists())

                 // if found we have an extra flow
                .on('FOUND').to(removePlatformMaybeFlow()).next(stepOf(this.&getInsertGplInfoTasklet))
                .from(checkPlatformExists()).next(stepOf(this.&getInsertGplInfoTasklet))

                .next(mainStep)
                .build()
    }

    @Bean
    Step checkPlatformExists() {
        steps.get('checkPlatformExists')
                .tasklet(platformCheckTasklet)
                .listener(showCountStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()
    }

    @Bean
    Flow removePlatformMaybeFlow() {
        Step platformDataCheckStep =  steps.get('checkPlatformDataExists')
                .tasklet(platformDataCheckTasklet)
                .listener(showCountStepListener())
                .listener(new FoundExitStatusChangeListener())
                .build()

        new FlowBuilder<Flow>('removePlatform')
                .start(platformDataCheckStep)
                .on('FOUND').fail()
                .next(stepOf(this.&getDeleteMrnaAnnotationTasklet))
                .next(stepOf(this.&getDeleteGplInfoTasklet))
                .build()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource annotationsFileResource() {
        new JobParameterFileResource(parameter: AbstractPlatformJobParameters.ANNOTATIONS_FILE)
    }

}
