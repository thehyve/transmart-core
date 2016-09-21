package org.transmartproject.batch.gwas

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobInterruptedException
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.context.StepSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.PathResource
import org.transmartproject.batch.batchartifacts.*
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.OverriddenNameStep
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.db.oracle.OraclePartitionTasklet
import org.transmartproject.batch.gwas.analysisdata.AssayAnalysisGwasWriter
import org.transmartproject.batch.gwas.analysisdata.DeleteCurrentGwasAnalysisDataTasklet
import org.transmartproject.batch.gwas.analysisdata.GwasAnalysisRow
import org.transmartproject.batch.gwas.analysisdata.GwasAnalysisRowValidator
import org.transmartproject.batch.gwas.analysisdata.UpdateBioAssayAnalysisCountListener
import org.transmartproject.batch.gwas.biodata.GwasBioExperimentWriter
import org.transmartproject.batch.gwas.metadata.*
import org.transmartproject.batch.support.ExpressionResolver
import org.transmartproject.batch.support.JobParameterFileResource

import javax.annotation.Resource

/**
 * Spring configuration for the GWAS job.
 */
@Configuration
@ComponentScan([
        'org.transmartproject.batch.gwas',
        'org.transmartproject.batch.biodata',
])
class GwasJobConfiguration extends AbstractJobConfiguration {

    @SuppressWarnings('NonFinalPublicField')
    public static int chunkSize = 50000

    public static final String JOB_NAME = 'GwasJob'

    @Resource
    Tasklet updateGwasTop500Tasklet

    @Bean(name = 'GwasJob')
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
    }

    @Bean
    Flow mainFlow() {
        def readMetaDataFileStep = wrapStepWithName('readMetadataStep',
                readMetadataStep(null))

        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readMetaDataFileStep)
                .next(wrapStepWithName('insertBioExperiments', insertBioExperiments(null, null)))
                .next(analysisInsertionOuterStep(null))
                .build()
    }

    @Bean
    Step analysisInsertionOuterStep(GwasAnalysisPartitioner partitioner) {
        // wrap inside RegisterStepScopeOnExecutionStep, so that when
        // Step::execute() is called the step scope is set up and the
        // the beans analysisInsertionInnerStep and analysisInsertionFlow
        // can be built
        def innerStep = new RegisterStepScopeOnExecutionStep(
                newName: 'analysisInsertionInner',
                step: analysisInsertionInnerStep())

        steps.get('analysesInsertionOuter')
                .partitioner(innerStep)
                .partitioner(innerStep.name, partitioner)
                .taskExecutor(new JobContextAwareTaskExecutor().with {
                    it.concurrencyLimit = Runtime.runtime.availableProcessors()
                    it
                })
                .build()
    }

    static class RegisterStepScopeOnExecutionStep extends OverriddenNameStep {
        void execute(StepExecution stepExecution) throws JobInterruptedException {
            try {
                StepSynchronizationManager.register(stepExecution)
                step.execute(stepExecution)
            } finally {
                StepSynchronizationManager.close()
            }
        }
    }

    @Bean
    @StepScope
    Step analysisInsertionInnerStep() {
        steps.get('analysisInsertionInner')
                .allowStartIfComplete(true)
                .flow(analysisInsertionFlow())
                .build()
    }

    @Bean
    @StepScope
    Flow analysisInsertionFlow(@Value("#{stepName}") stepName) {
        // prepend step name with the name of the partition so that
        // we don't have steps deemed to be re-run.
        def w = { wrapStepWithName(stepName + '-' + it.name, it) }
        new FlowBuilder<SimpleFlow>('analysisInsertionFlow')
                .start(w(insertBioAssayAnalysisStep(null)))
                .next(w(stepOf(this.&databasePartitionTasklet)))
                .next(w(deleteCurrentAnalysisDataStep(null)))
                .next(w(insertIntoBioAssayAnalysisGwasStep(null, null, null, null)))
                .next(w(stepOf(this.&getUpdateGwasTop500Tasklet)))
                .build()
    }

    @Bean
    @JobScopeInterfaced
    Step readMetadataStep(GwasMetadataStore metadataStore) {
        def reader = tsvFileReader(
                metaDataFile(),
                beanClass: GwasMetadataEntry,
                columnNames: 'auto',
                linesToSkip: 1,
                saveState: false,
                emptyStringsToNull: true)


        steps.get('readMetadataStep')
                .chunk(5)
                .reader(reader)
                .processor(compositeOf(
                        new ValidatingItemProcessor(jsr303Validator()),
                        duplicateAnalysisDetector(),
                        duplicateFileDetector()))
                .writer(new PutInBeanWriter(bean: metadataStore))
                .listener(logCountsStepListener())
                .listener(metadataStore)
                .listener(lineOfErrorDetectionListener())
                .build()
    }


    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource metaDataFile() {
        new JobParameterFileResource(
                parameter: GwasParameterModule.META_DATA_FILE)
    }

    @Bean
    @JobScopeInterfaced
    Step insertBioExperiments(GwasBioExperimentWriter gwasBioExperimentWriter,
                              ExpressionResolver expressionResolver) {
        steps.get('insertBioExperiments')
                .chunk(5)
                .reader(new IterableItemReader(
                        name: 'gwasMetadataStoreReader',
                        expressionResolver: expressionResolver,
                        expression: "@gwasMetadataStore.studies"))
                .writer(gwasBioExperimentWriter)
                .listener(logCountsStepListener())
                .build()
    }

    private ItemProcessor<GwasMetadataEntry, GwasMetadataEntry> duplicateAnalysisDetector() {
        new DuplicationDetectionProcessor<GwasMetadataEntry>(
                calculateKey: { GwasMetadataEntry entry ->
                    new Tuple(entry.study, entry.analysisName)
                })
    }

    private ItemProcessor<GwasMetadataEntry, GwasMetadataEntry> duplicateFileDetector() {
        new DuplicationDetectionProcessor<GwasMetadataEntry>(
                calculateKey: { GwasMetadataEntry entry ->
                    entry.inputFile
                })
    }

    @Bean
    @JobScopeInterfaced
    Step insertBioAssayAnalysisStep(InsertBioAssayAnalysisTasklet insertBioAssayAnalysisTasklet) {
        steps.get('insertBioAssayAnalysisTasklet')
                .allowStartIfComplete(true)
                .tasklet(insertBioAssayAnalysisTasklet)
                .listener(new LogCountsStepListener())
                .build()
    }

    @Bean
    @StepScopeInterfaced
    Tasklet databasePartitionTasklet(
            @Value("#{currentGwasAnalysisContext.bioAssayAnalysisId}") Long analysisId) {
        assert analysisId != null

        switch (picker.pickClass(PostgresPartitionTasklet, OraclePartitionTasklet)) {
            case PostgresPartitionTasklet:
                return new PostgresPartitionTasklet(
                        tableName: Tables.BIO_ASSAY_ANALYSIS_GWAS,
                        partitionByColumn: 'bio_assay_analysis_id',
                        partitionByColumnValue: analysisId,
                        sequence: Sequences.BIO_DATA_ID,
                        indexes: [['rs_id']])
            case OraclePartitionTasklet:
                return new OraclePartitionTasklet(
                        tableName: Tables.BIO_ASSAY_ANALYSIS_GWAS,
                        partitionByColumnValue: analysisId)
            default:
                return null
        }


    }

    @Bean
    Step deleteCurrentAnalysisDataStep(
            DeleteCurrentGwasAnalysisDataTasklet deleteCurrentGwasAnalysisDataTasklet) {
        stepOf('deleteCurrentAnalysisData', deleteCurrentGwasAnalysisDataTasklet)
    }

    @Bean
    @JobScopeInterfaced
    Step insertIntoBioAssayAnalysisGwasStep(AssayAnalysisGwasWriter assayAnalysisGwasWriter,
                                            CurrentGwasAnalysisContext gwasAnalysisContext,
                                            GwasAnalysisRowValidator gwasAnalysisRowValidator,
                                            UpdateBioAssayAnalysisCountListener updateBioAssayAnalysisCountListener) {
        steps.get('insertIntoBioAssayAnalysisGwas')
                .chunk(chunkSize)
                .reader(gwasDataFileReader())
                .processor(new ValidatingItemProcessor(adaptValidator(gwasAnalysisRowValidator)))
                .writer(assayAnalysisGwasWriter)
                .listener(progressWriteListener())
                .listener(gwasAnalysisContext.updateRowCountListener)
                .listener(updateBioAssayAnalysisCountListener)
                .build()
    }

    @Bean
    @StepScope
    FlatFileItemReader<GwasAnalysisRow> gwasDataFileReader() {
        // the reader cannot be shared across steps, as the same
        // type of step will be running at the same time in different threads
        // Hence, the @StepScope

        // a lot of time is spent on mapping the FieldSet into a bean
        // to optimize this, we could write our own code for this task
        tsvFileReader(
                gwasDataFileResource(null, null),
                beanClass: GwasAnalysisRow,
                columnNames: 'auto',
                linesToSkip: 1,
                saveState: true,
                emptyStringsToNull: true)
    }

    @Bean
    @StepScopeInterfaced
    @SuppressWarnings('JavaIoPackageAccess')
    org.springframework.core.io.Resource gwasDataFileResource(
            @Value("#{jobParameters['DATA_LOCATION']}") File dataLocation,
            @Value('#{currentGwasAnalysisContext.metadataEntry.inputFile}') File inputFile) {
        File file
        if (inputFile.absolute) {
            file = inputFile
        } else {
            file = new File(dataLocation, inputFile.toString())
        }
        new PathResource(file.absolutePath)
    }


    @Override
    protected void configure(SequenceReserver sequenceReserver) {
        sequenceReserver.configureBlockSize(Sequences.BIO_DATA_ID, chunkSize)
    }
}
