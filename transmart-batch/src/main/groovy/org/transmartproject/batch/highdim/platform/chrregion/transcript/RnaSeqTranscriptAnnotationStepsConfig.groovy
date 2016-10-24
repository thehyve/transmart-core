package org.transmartproject.batch.highdim.platform.chrregion.transcript

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.FlatFileItemReader
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
 * Spring configuration for the transcript data job.
 */
@Configuration
@ComponentScan
class RnaSeqTranscriptAnnotationStepsConfig implements StepBuildingConfigurationTrait {

    static int chunkSize = 5000

    @Bean
    Step loadAnnotationMappings(ItemStreamReader<AnnotationEntity> transcriptAnnotReader) {
        steps.get('loadAnnotationMappings')
                .allowStartIfComplete(true)
                .chunk(100)
                .reader(transcriptAnnotReader)
                .writer(new PutInBeanWriter(bean: annotationEntityMap()))
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    Step deleteRnaSeqTranscriptAnnotations(Tasklet deleteRnaSeqTranscriptAnnotationTasklet) {
        stepOf('deleteRnaSeqTranscriptAnnotationTasklet', deleteRnaSeqTranscriptAnnotationTasklet)
    }

    @Bean
    Step insertRnaSeqTranscriptAnnotations(
            RnaSeqTranscriptAnnotationWriter rnaSeqTranscriptAnnotationWriter,
            FlatFileItemReader<RnaSeqTranscriptAnnotationRow> rnaSeqTranscriptAnnotationReader) {

        steps.get('insertRnaSeqTranscriptAnnotations')
                .chunk(chunkSize)
                .reader(rnaSeqTranscriptAnnotationReader)
                .writer(rnaSeqTranscriptAnnotationWriter)
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource annotationsFileResource() {
        new JobParameterFileResource(parameter: AbstractPlatformJobSpecification.ANNOTATIONS_FILE)
    }

    @Bean
    @JobScope
    FlatFileItemReader<RnaSeqTranscriptAnnotationRow> rnaSeqTranscriptAnnotationReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader(
                annotationsFileResource,
                linesToSkip: 1,
                beanClass: RnaSeqTranscriptAnnotationRow,
                columnNames: ['gplId', 'chromosome', 'start', 'end', 'transcriptId'])
    }

    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader rnaSeqTranscriptAnnotationReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.RNASEQ_TRANSCRIPT_ANNOTATION,
                idColumn: 'id',
                //FIXME
                nameColumn: 'transcript',
        )
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }
}
