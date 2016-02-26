package org.transmartproject.batch.highdim.mrna.platform

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntity
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring configuration for the mrna platform specific steps.
 */
@Configuration
@ComponentScan
class MrnaPlatformStepsConfig implements StepBuildingConfigurationTrait {

    static int chunkSize = 5000

    @Bean
    Step loadAnnotationMappings(ItemStreamReader<AnnotationEntity> annotationsReader) {
        steps.get('loadAnnotationMappings')
                .allowStartIfComplete(true)
                .chunk(100)
                .reader(annotationsReader)
                .writer(new PutInBeanWriter(bean: annotationEntityMap()))
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    Step deleteAnnotations(Tasklet deleteMrnaAnnotationTasklet) {
        stepOf('deleteAnnotations', deleteMrnaAnnotationTasklet)
    }

    @Bean
    Step insertAnnotations(
            MrnaAnnotationRowValidator annotationRowValidator,
            ItemWriter<MrnaAnnotationRow> mrnaAnnotationWriter) {
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
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.MRNA_ANNOTATION,
                idColumn: 'probeset_id',
                nameColumn: 'probe_id',
        )
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource annotationsFileResource() {
        new JobParameterFileResource(parameter: AbstractPlatformJobSpecification.ANNOTATIONS_FILE)
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
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }
}
