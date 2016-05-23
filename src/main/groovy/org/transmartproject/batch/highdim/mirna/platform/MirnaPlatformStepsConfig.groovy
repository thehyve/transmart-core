package org.transmartproject.batch.highdim.mirna.platform

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
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
 * Spring configuration for the miRNA platform specific steps.
 */
@Configuration
@ComponentScan
class MirnaPlatformStepsConfig implements StepBuildingConfigurationTrait {

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
    Step deleteAnnotations(Tasklet deleteMirnaAnnotationTasklet) {
        stepOf('deleteAnnotations', deleteMirnaAnnotationTasklet)
    }

    @Bean
    ItemProcessor<MirnaAnnotationRow, MirnaAnnotationRow> compositeMirnaAnnotationRowProcessor(
            MirnaAnnotationRowValidator annotationRowValidator
    ) {
        compositeOf(
                new LowerCaseMirnaIdItemProcessor(),
                new ValidatingItemProcessor(adaptValidator(annotationRowValidator))
        )
    }

    @Bean
    Step insertAnnotations(
            ItemReader<MirnaAnnotationRow> mirnaAnnotationRowReader,
            ItemProcessor<MirnaAnnotationRow, MirnaAnnotationRow> compositeMirnaAnnotationRowProcessor,
            ItemWriter<MirnaAnnotationRow> mirnaAnnotationWriter) {
        steps.get('mainStep')
                .chunk(chunkSize)
                .reader(mirnaAnnotationRowReader)
                .processor(compositeMirnaAnnotationRowProcessor)
                .writer(mirnaAnnotationWriter)
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.MIRNA_ANNOTATION,
                idColumn: 'probeset_id',
                nameColumn: 'id_ref',
        )
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource annotationsFileResource() {
        new JobParameterFileResource(parameter: AbstractPlatformJobSpecification.ANNOTATIONS_FILE)
    }

    @Bean
    @JobScope
    FlatFileItemReader<MirnaAnnotationRow> mirnaAnnotationRowReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader([
                beanClass  : MirnaAnnotationRow,
                columnNames: ['idRef', 'mirnaId'],
                linesToSkip: 1
        ], annotationsFileResource)
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }
}
