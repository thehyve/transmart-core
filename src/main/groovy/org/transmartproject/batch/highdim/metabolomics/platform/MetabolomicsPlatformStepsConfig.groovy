package org.transmartproject.batch.highdim.metabolomics.platform

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.ClosureFilterProcessor
import org.transmartproject.batch.batchartifacts.DuplicationDetectionProcessor
import org.transmartproject.batch.batchartifacts.IterableItemReader
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.metabolomics.platform.model.Biochemical
import org.transmartproject.batch.highdim.metabolomics.platform.writers.BiochemicalSubPathwayAssociationWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.BiochemicalsWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.SubPathwaysWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.SuperPathwaysWriter
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntity
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.support.ExpressionResolver

/**
 * Spring configuration for the metabolomics platform steps.
 */
@Configuration
@ComponentScan
class MetabolomicsPlatformStepsConfig implements StepBuildingConfigurationTrait {

    static int chunkSize = 5000

    @Autowired
    ExpressionResolver expressionResolver

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
    Step deleteMetabolomicsAnnotation(Tasklet deleteMetabolomicsAnnotationTasklet) {
        stepOf('deleteMetabolomicsAnnotation', deleteMetabolomicsAnnotationTasklet)
    }

    @Bean
    Step readTheGraph(MetabolomicsAnnotationRowValidator validator) {
        steps.get('firstPass')
                .chunk(chunkSize)
                .reader(metabolomicsAnnotationFileReader(null))
                .processor(compositeOf(
                new ValidatingItemProcessor(adaptValidator(validator)),
                duplicateBiochemicalProcessor(),
                duplicateHmdbProcessor()))
                .writer(writeToMemory(null))
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    Step metabolomicsAssignIds(Tasklet metabolomicsAssignIdsTasklet) {
        stepOf('metabolomicsAssignIds', metabolomicsAssignIdsTasklet)
    }

    @Bean
    @JobScope
    // because of the iterableItemReader
    Step insertSuperPathways(SuperPathwaysWriter superPathwaysWriter) {
        insertStep 'insertSuperPathways', 'superPathways', superPathwaysWriter
    }

    @Bean
    @JobScope
    Step insertSubPathways(SubPathwaysWriter subPathwaysWriter) {
        insertStep 'insertSubPathways', 'subPathways', subPathwaysWriter
    }

    @Bean
    @JobScope
    Step insertBiochemicals(BiochemicalsWriter biochemicalsWriter) {
        insertStep 'insertBiochemicals', 'biochemicals', biochemicalsWriter
    }

    @Bean
    @JobScope
    Step insertBiochemicalsSubPathwaysAssociations(
            BiochemicalSubPathwayAssociationWriter biochemicalSubPathwayAssociationWriter) {
        insertStep 'insertBiochemicalsSubpathwaysAssociations',
                'biochemicals', biochemicalSubPathwayAssociationWriter,
                new ClosureFilterProcessor<Biochemical>({ Biochemical c ->
                    c.subPathway != null
                })
    }

    private Step insertStep(String name,
                            String pileProperty,
                            ItemWriter writer,
                            ItemProcessor processor = null) {
        // IterableItemReader has state, step should be job scoped
        steps.get(name)
                .chunk(chunkSize)
                .reader(new IterableItemReader(
                saveState: false,
                expressionResolver: expressionResolver,
                expression: "@metabolomicsBiochemicalsPile.$pileProperty"))
                .processor(processor)
                .writer(writer)
                .listener(progressWriteListener())
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    @JobScope
    FlatFileItemReader<MetabolomicsAnnotationRow> metabolomicsAnnotationFileReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader(
                annotationsFileResource,
                beanClass: MetabolomicsAnnotationRow,
                columnNames: 'auto',
                linesToSkip: 1,
        )
    }

    @Bean
    @JobScopeInterfaced
    ItemProcessor duplicateBiochemicalProcessor() {
        new DuplicationDetectionProcessor(
                saveState: false,
                calculateKey: { MetabolomicsAnnotationRow row ->
                    row.biochemical
                })
    }

    @Bean
    @JobScopeInterfaced
    ItemProcessor duplicateHmdbProcessor() {
        new DuplicationDetectionProcessor(
                saveState: false,
                throwOnRepeated: false, // only warn
                calculateKey: { MetabolomicsAnnotationRow row ->
                    row.hmdbId
                })
    }

    @Bean
    ItemWriter<MetabolomicsAnnotationRow> writeToMemory(
            MetabolomicsBiochemicalsPile pile) {
        new PutInBeanWriter<MetabolomicsAnnotationRow>(bean: pile)
    }

    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.METAB_ANNOTATION,
                idColumn: 'id',
                nameColumn: 'biochemical_name',
        )
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }
}
