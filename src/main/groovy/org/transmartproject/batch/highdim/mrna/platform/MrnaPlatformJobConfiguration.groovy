package org.transmartproject.batch.highdim.mrna.platform

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.highdim.platform.PlatformLoadJobConfiguration

import javax.annotation.Resource

/**
 * Spring configuration for the clinical data job.
 */
@Configuration
@ComponentScan('org.transmartproject.batch.highdim.mrna.platform')
class MrnaPlatformJobConfiguration extends PlatformLoadJobConfiguration {

    public static final String JOB_NAME = 'MrnaPlatformLoadJob'

    static int chunkSize = 5000

    @Resource
    Tasklet deleteMrnaAnnotationTasklet

    @Autowired
    MrnaAnnotationRowValidator annotationRowValidator

    @Autowired
    MrnaAnnotationWriter mrnaAnnotationWriter

    @Bean(name = 'MrnaPlatformLoadJob' /* JOB_NAME */)
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow(null))
                .end()
                .build()
    }

    @Override
    protected void configure(SequenceReserver sequenceReserver) {
        sequenceReserver.configureBlockSize(Sequences.PROBESET_ID, chunkSize)
    }

    @Bean
    Step mainStep() {
        steps.get('mainStep')
                .chunk(chunkSize)
                .reader(mrnaAnnotationRowReader(null))
                .processor(new ValidatingItemProcessor(
                adaptValidator(annotationRowValidator)))
                .writer(mrnaAnnotationWriter)
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScope
    FlatFileItemReader<MrnaAnnotationRow> mrnaAnnotationRowReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader(
                annotationsFileResource,
                beanClass: MrnaAnnotationRow,
                columnNames: ['gplId', 'probeName', 'genes',
                              'entrezIds', 'organism'])
    }

    @Bean
    Step deleteAnnotationsStep() {
        stepOf(this.&getDeleteMrnaAnnotationTasklet)
    }

}
