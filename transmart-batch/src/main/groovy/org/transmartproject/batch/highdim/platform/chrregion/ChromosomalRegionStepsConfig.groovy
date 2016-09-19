package org.transmartproject.batch.highdim.platform.chrregion

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.datastd.ChromosomalRegionValidator
import org.transmartproject.batch.highdim.datastd.PlatformValidator
import org.transmartproject.batch.highdim.platform.AbstractPlatformJobSpecification
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntity
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring configuration for the chromosomal region data job.
 */
@Configuration
@ComponentScan
class ChromosomalRegionStepsConfig implements StepBuildingConfigurationTrait {

    static int chunkSize = 5000

    @Bean
    Step loadAnnotationMappings(ItemStreamReader<AnnotationEntity> chromosomalRegionReader) {
        steps.get('loadAnnotationMappings')
                .allowStartIfComplete(true)
                .chunk(100)
                .reader(chromosomalRegionReader)
                .writer(new PutInBeanWriter(bean: annotationEntityMap()))
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    Step deleteChromosomalRegions(Tasklet deleteChromosomalRegionTasklet) {
        stepOf('deleteChromosomalRegions', deleteChromosomalRegionTasklet)
    }

    @Bean
    @JobScope
    PlatformValidator platformOrganismValidator() {
        new PlatformValidator()
    }

    @Bean
    @JobScope
    ChromosomalRegionValidator chromosomalRegionValidator() {
        new ChromosomalRegionValidator()
    }

    @Bean
    ItemProcessor<ChromosomalRegionRow, ChromosomalRegionRow> compositeChromosomalRegionRowValidatingProcessor(
            PlatformValidator platformOrganismValidator,
            ChromosomalRegionValidator chromosomalRegionValidator,
            ChromosomalRegionRowValidator chromosomalRegionRowValidator

    ) {
        compositeOf(
                new ValidatingItemProcessor(adaptValidator(platformOrganismValidator)),
                new ValidatingItemProcessor(adaptValidator(chromosomalRegionValidator)),
                new ValidatingItemProcessor(adaptValidator(chromosomalRegionRowValidator))
        )
    }

    @Bean
    Step insertChromosomalRegions(
            ItemProcessor compositeChromosomalRegionRowValidatingProcessor,
            ChromosomalRegionRowWriter chromosomalRegionRowWriter,
            FlatFileItemReader<ChromosomalRegionRow> chromosomalRegionRowReader) {

        steps.get('insertChromosomalRegions')
                .chunk(chunkSize)
                .reader(chromosomalRegionRowReader)
                .processor(compositeChromosomalRegionRowValidatingProcessor)
                .writer(chromosomalRegionRowWriter)
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
    FlatFileItemReader<ChromosomalRegionRow> chromosomalRegionRowReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader(
                annotationsFileResource,
                linesToSkip: 1,
                beanClass: ChromosomalRegionRow,
                columnNames: ['gplId', 'regionName', 'chromosome', 'startBp', 'endBp', 'numProbes', 'cytoband',
                              'geneSymbol', 'geneId', 'organism'])
    }

    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader chromosomalRegionReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.CHROMOSOMAL_REGION,
                idColumn: 'region_id',
                nameColumn: 'region_name',
        )
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }
}
