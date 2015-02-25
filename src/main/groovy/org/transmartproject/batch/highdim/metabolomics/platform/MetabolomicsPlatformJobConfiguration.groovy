package org.transmartproject.batch.highdim.metabolomics.platform

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.FlowJob
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
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
import org.transmartproject.batch.highdim.metabolomics.platform.model.Biochemical
import org.transmartproject.batch.highdim.metabolomics.platform.writers.BiochemicalSubPathwayAssociationWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.BiochemicalsWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.SubPathwaysWriter
import org.transmartproject.batch.highdim.metabolomics.platform.writers.SuperPathwaysWriter
import org.transmartproject.batch.highdim.platform.PlatformLoadJobConfiguration
import org.transmartproject.batch.support.ExpressionResolver

import javax.annotation.Resource

/**
 * Spring configuration for the clinical data job.
 */
@Configuration
@ComponentScan('org.transmartproject.batch.highdim.metabolomics.platform')
class MetabolomicsPlatformJobConfiguration extends PlatformLoadJobConfiguration {

    public static final String JOB_NAME = 'MetabolomicsPlatformLoadJob'

    static int chunkSize = 5000

    @Resource
    Tasklet deleteMetabolomicsAnnotationTasklet

    @Resource
    Tasklet metabolomicsAssignIdsTasklet

    @Autowired
    ExpressionResolver expressionResolver

    @Bean(name = 'MetabolomicsPlatformLoadJob' /* JOB_NAME */)
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
                .with { FlowJob job -> job.restartable = false; job }
    }

    @Bean
    @Override
    Step mainStep() {
        /*
         * Two passes on the file
         * 1. first pass validates and builds the graph in memory
         * 2. then the ids are assigned
         * 3. finally the graph is written into memory
         *    a. first the super pathways
         *    b. then the sub pathways
         *    c. the metabolites
         *    d. the associations between the metabolites and the sub pathways
         */
        steps.get('metabolomicsPlatformPasses')
                .flow(metabolomicsPlatformPassesFlow())
                .build()
    }

    @Bean
    @JobScope // to avoid having to use wrapStepWithName
    Flow metabolomicsPlatformPassesFlow() {
        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(firstPass())
                .next(stepOf(this.&getMetabolomicsAssignIdsTasklet))
                .next(insertSuperPathwaysStep())
                .next(insertSubPathwaysStep())
                .next(insertBiochemicals())
                .next(insertBiochemicalsSubPathwaysAssociations())
                .build()
    }

    @Bean
    Step firstPass(MetabolomicsAnnotationRowValidator validator) {
        steps.get('firstPass')
                .chunk(chunkSize)
                .reader(metabolomicsAnnotationFileReader(null))
                .processor(compositeOf(
                        new ValidatingItemProcessor(adaptValidator(validator)),
                        duplicateBiochemicalProcessor(),
                        duplicateHmdbProcessor()))
                .writer(firstPassWriter())
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    private Step insertStep(String name,
                            String pileProperty,
                            ItemWriter writer,
                            ItemProcessor processor = null) {
        // IterableItemReader has state, step should be job scoped
        steps.get(name)
                .chunk(chunkSize)
                .reader(new IterableItemReader(
                expressionResolver: expressionResolver,
                expression: "@metabolomicsBiochemicalsPile.$pileProperty"))
                .processor(processor)
                .writer(writer)
                .listener(progressWriteListener())
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    @JobScope // because of the iterableItemReader
    Step insertSuperPathwaysStep(SuperPathwaysWriter superPathwaysWriter) {
        insertStep 'insertSuperPathways', 'superPathways', superPathwaysWriter
    }

    @Bean
    @JobScope
    Step insertSubPathwaysStep(SubPathwaysWriter subPathwaysWriter) {
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
                calculateKey: { MetabolomicsAnnotationRow row ->
                    row.hmdbId
                })
    }

    @Bean // singleton should be fine here
    ItemWriter<MetabolomicsAnnotationRow> firstPassWriter(
            MetabolomicsBiochemicalsPile pile) {
        new PutInBeanWriter<MetabolomicsAnnotationRow>(bean: pile)
    }

    @Bean
    @Override
    Step deleteAnnotationsStep() {
        stepOf(this.&getDeleteMetabolomicsAnnotationTasklet)
    }
}
