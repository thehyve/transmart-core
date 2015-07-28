package org.transmartproject.batch.i2b2

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.MultiResourceItemReader
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.DuplicationDetectionProcessor
import org.transmartproject.batch.batchartifacts.StringTemplateBasedDecider
import org.transmartproject.batch.batchartifacts.NullWriter
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.i2b2.codelookup.CodeLookupLoadingTasklet
import org.transmartproject.batch.i2b2.dimensions.*
import org.transmartproject.batch.i2b2.firstpass.I2b2FirstPassSplittingReader
import org.transmartproject.batch.i2b2.firstpass.VariableAndDataPointValidator
import org.transmartproject.batch.i2b2.mapping.*
import org.transmartproject.batch.i2b2.secondpass.*
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable
import org.transmartproject.batch.support.JobParameterFileResource

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable.PROVIDER_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Spring configuration for the clinical data job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.i2b2',])
class I2b2JobConfiguration extends AbstractJobConfiguration {

    public static final String JOB_NAME = 'I2b2Job'

    static int firstPassChunkSize = 50000
    static int findDimensionsChunkSize = 1000 - 1 /* higher is problematic for oracle */
    static int insertDimensionsChunkSize = 5000
    static int secondPassChunkSize = 10000

    private final static String DIMENSIONS_STORE_JOB_KEY = 'dimensionsStore'
    public static final String COMPLETED_NEXT_INCREMENTAL = 'COMPLETED PROCEED INCREMENTAL'

    @Bean(name = 'I2b2Job')
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow(null, null, null))
                .end()
                .build()
    }

    @Bean
    Flow mainFlow(Tasklet deleteI2b2DataTasklet,
                  Tasklet assignDimensionIdsTasklet,
                  JobExecutionDecider incrementalDecider) {
        def readVariablesStep = wrapStepWithName('readVariables',
                readVariablesStep(null, null, null, null))
        def firstPassStep = wrapStepWithName('firstPass',
                firstPass(null, null))
        def secondPassStep = wrapStepWithName('secondPass',
                secondPass(null, null, null, null, null))
        def assignDimensionIdsStep = stepOf('assignDimensionIds',
                assignDimensionIdsTasklet)
        def findCurrentDimensionsStep = wrapStepWithName('findCurrentDimensions',
                findCurrentDimensionsStep(null))

        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readVariablesStep) //reads control files (column map and word map)
                .next(readWordMappingsStep(null, null))
                .next(allowStartStepOf(this.&loadCodeLookupsTasklet))
                .next(firstPassStep)
                .next(incrementalDecider)
                .on(COMPLETED_NEXT_INCREMENTAL)
                .to(findCurrentDimensionsStep)                       // 5.b
                .next(assignDimensionIdsStep)
                .from(incrementalDecider)
                .on(ExitStatus.COMPLETED.exitCode)
                .to(stepOf('deleteI2b2Data', deleteI2b2DataTasklet)) // 5.a
                .next(assignDimensionIdsStep)
                .next(insertDimensionObjectsFlow(null, null, null, null, null))
                .next(secondPassStep)
                .build()
    }

    @Bean
    JobExecutionDecider incrementalDecider() {
        new StringTemplateBasedDecider(
                '${params.INCREMENTAL == "Y" ? ' +
                        "'$COMPLETED_NEXT_INCREMENTAL' : " +
                        "'${ExitStatus.COMPLETED.exitCode}'}")
    }

    /*
     * Context beans
     */
    @Bean
    @JobScope
    DimensionsStore dimensionsStore(
            @Value('#{jobExecution.executionContext}') ExecutionContext jobExecutionContext) {
        if (jobExecutionContext.get(DIMENSIONS_STORE_JOB_KEY) == null) {
            def newObject = new DimensionsStore()
            jobExecutionContext.put(DIMENSIONS_STORE_JOB_KEY, newObject)
        }

        /*
         * TODO
         * Maybe duplicate here instead? Working directly with the context
         * object means that changes to the object that happen in rolled
         * back transactions will be persisted to the database and restored
         * when the job is restarted. OTOH:
         * a) during the 1st pass, the dimensions store is persisted on each
         *    chunk commit (see SnapshotDimensionsStoreItemStream)
         * b) during the findCurrentDimensionsStep, an error midway will commit
         *    partial results to the job execution context. But the reader of
         *    that step is not resumable (doesn't implement ItemStream) and
         *    will overwrite all the state was changed in the previous
         *    execution.
         * c) The id attribution to dimension objects step is also not a
         *    problem. The ids already attribute can be kept or reassigned
         *    (they're kept).
         * d) The remaining two steps do not modify the dimensions store.
         *
         * The advantage of *not* duplicating is that no special code is needed
         * to put the changed store in the context.
         */
        jobExecutionContext.get(DIMENSIONS_STORE_JOB_KEY)
    }

    /*
     * 1. Column Mapping
     */

    @Bean
    @JobScopeInterfaced
    Step readVariablesStep(I2b2MappingEntryLocalValidator i2b2MappingEntryLocalValidator,
                           ItemProcessor<I2b2MappingEntry, I2b2MappingEntry> i2b2VariableBindingProcessor,
                           ItemProcessor<I2b2MappingEntry, I2b2MappingEntry> fileResourceBindingProcessor,
                           I2b2MappingStore i2b2MappingStore) {
        /* filename column_number variable type unit mandatory */
        def reader = tsvFileReader(
                columnMapFile(),
                beanClass: I2b2MappingEntry,
                columnNames: 'auto',
                linesToSkip: 1,
                saveState: false,
                emptyStringsToNull: true)


        steps.get('readColumnMappingFile')
                .allowStartIfComplete(true)
                .chunk(5)
                .reader(reader)
                .processor(compositeOf(
                        i2b2VariableBindingProcessor,
                        fileResourceBindingProcessor,
                        new ValidatingItemProcessor(adaptValidator(i2b2MappingEntryLocalValidator)),
                        duplicateColumnMappingEntryDetector()
                ))
                .writer(new PutInBeanWriter(bean: i2b2MappingStore))
                .listener(logCountsStepListener())
                .listener(i2b2MappingStore.columnMappingsPostProcessingListener)
                .build()
    }

    private ItemProcessor<I2b2MappingEntry, I2b2MappingEntry> duplicateColumnMappingEntryDetector() {
        // warn only; there are legitimate reasons to map the same data file
        // column multiple times
        new DuplicationDetectionProcessor<I2b2MappingEntry>(
                throwOnRepeated: false,
                saveState: false,
                calculateKey: { I2b2MappingEntry entry ->
                    new Tuple(entry.filename, entry.columnNumber)
                })
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource columnMapFile() {
        new JobParameterFileResource(
                parameter: I2b2ParametersModule.COLUMN_MAP_FILE)
    }

    /*
     * 2. Word Mapping
     */

    @Bean
    Step readWordMappingsStep(I2b2WordMappingValidator i2b2WordMappingValidator,
                              I2b2MappingStore i2b2MappingStore) {
        def reader = tsvFileReader(
                wordMappingFile(),
                beanClass: I2b2WordMapping,
                strict: false,
                columnNames: 'auto',
                linesToSkip: 1,
                saveState: false,)

        steps.get('readWordMappings')
                .allowStartIfComplete(true)
                .chunk(5)
                .reader(reader)
                .processor(compositeOf(
                        new ValidatingItemProcessor(adaptValidator(i2b2WordMappingValidator)),
                        duplicateWordMappingDetector()
                ))
                .writer(new PutInBeanWriter(bean: i2b2MappingStore))
                .listener(logCountsStepListener())
                .build()
    }

    private ItemProcessor<I2b2WordMapping, I2b2WordMapping> duplicateWordMappingDetector() {
        new DuplicationDetectionProcessor<I2b2WordMapping>(
                saveState: false,
                calculateKey: { I2b2WordMapping entry ->
                    new Tuple(entry.filename, entry.columnNumber, entry.from)
                }
        )
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource wordMappingFile() {
        new JobParameterFileResource(
                parameter: I2b2ParametersModule.WORD_MAP_FILE)
    }

    /*
     * 3. Load code lookups
     */

    @Bean
    @StepScope // has to be bound quite late
    Tasklet loadCodeLookupsTasklet(I2b2MappingStore mappingStore) {
        new CodeLookupLoadingTasklet(
                dimensionVariables: mappingStore.allEntries*.i2b2Variable
                        .findAll { it instanceof DimensionI2b2Variable })
    }

    /*
     * 4. First pass
     */
    @Bean
    @JobScope // has to be bound late because of the mappingStore call
    Step firstPass(VariableAndDataPointValidator variableAndDataPointValidator,
                   SnapshotDimensionsStoreItemStream snapshotDimensionsStoreItemStream) {
        steps.get('firstPass')
                .chunk(firstPassChunkSize)
                .reader(firstPassReader(null))
                .processor(compositeOf(
                        new ValidatingItemProcessor(adaptValidator(variableAndDataPointValidator)),
                 ))
                .writer(new NullWriter())
                .stream(snapshotDimensionsStoreItemStream)
                .listener(dimensionsStore(null).getLogStatusListener(false))
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScope
    I2b2FirstPassSplittingReader firstPassReader(I2b2MappingStore mappingStore) {
        def readerDelegateDelegate = tsvFileReader(
                linesToSkip: 1,
                allowMissingTrailingColumns: false,
                columnNames: 'auto',
                null)
        def readerDelegate = new MultiResourceItemReader<FieldSet>(
                delegate: readerDelegateDelegate,
                strict: true,
                resources: mappingStore.allEntries*.fileResource as Set)
        new I2b2FirstPassSplittingReader(delegate: readerDelegate)
    }

    /*
     * 5. a) (non-incremental only) Delete everything
     */
    // auto-configured

    /*
     * 5. b) (incremental only) Identify current ids of patients and visits (if any);
     *       check if providers already exist
     */
    @Bean
    Flow findCurrentDimensionsFlow(RegisterExistingPatientsWriter registerExistingPatientsWriter,
                                   RegisterExistingVisitsWriter registerExistingVisitsWriter,
                                   RegisterExistingProvidersWriter registerExistingProvidersWriter) {
        // these will change concurrently the in the DimensionStore,
        // but synchronization is not needed due to the way the work is partitioned
        parallelFlowOf(
                'findCurrentDimensions',
                findCurrentDimensionObjectsStep(PATIENT_DIMENSION_KEY,
                        patientExternalIdsReader(),
                        registerExistingPatientsWriter),
                findCurrentDimensionObjectsStep(VISITS_DIMENSION_KEY,
                        visitExternalIdsReader(),
                        registerExistingVisitsWriter),
                findCurrentDimensionObjectsStep(PROVIDER_DIMENSION_KEY,
                        providerExternalIdsReader(),
                        registerExistingProvidersWriter))
    }

    @Bean
    @JobScope // because of the call to dimensionsStore
    Step findCurrentDimensionsStep(DimensionsStore dimensionsStore) {
        steps.get('findCurrentDimensions')
                .flow(findCurrentDimensionsFlow(null, null, null))
                .listener(dimensionsStore.getLogStatusListener(true))
                .build()
    }

    @Bean
    @JobScope
    DimensionObjectExternalIdReader patientExternalIdsReader() {
        new DimensionObjectExternalIdReader(
                dimensionsStore: dimensionsStore(),
                dimensionKey:    PATIENT_DIMENSION_KEY)
    }

    @Bean
    @JobScope
    DimensionObjectExternalIdReader visitExternalIdsReader() {
        new DimensionObjectExternalIdReader(
                dimensionsStore: dimensionsStore(),
                dimensionKey:    VISITS_DIMENSION_KEY)
    }

    @Bean
    @JobScope
    DimensionObjectExternalIdReader providerExternalIdsReader() {
        new DimensionObjectExternalIdReader(
                dimensionsStore: dimensionsStore(),
                dimensionKey:    PROVIDER_DIMENSION_KEY)
    }

    private Step findCurrentDimensionObjectsStep(String dimensionKey,
                                                 ItemReader<String> reader,
                                                 ItemWriter<String> writer) {

        steps.get("findCurrentDimensions-$dimensionKey")
                .chunk(findDimensionsChunkSize)
                .reader(reader)
                .writer(writer)
                .listener(logCountsStepListener())
                .build()
    }

    /*
     * 6. Assign ids to patients, visits and providers.
     */
    // configured automatically

    /*
     * 7. Insert patients, visits and providers
     */
    @Bean
    Flow insertDimensionObjectsFlow(DimensionsStore dimensionsStore,
                                    ItemProcessor<DimensionsStoreEntry, DimensionsStoreEntry>
                                            filterOutInsertedEntriesProcessor,
                                    ItemWriter<DimensionsStoreEntry> insertProvidersWriter,
                                    ItemWriter<DimensionsStoreEntry> insertPatientsWriter,
                                    ItemWriter<DimensionsStoreEntry> insertVisitsWriter) {
        def insertProvidersStep = steps.get('insertProviders')
                .chunk(insertDimensionsChunkSize)
                .reader(new DimensionsStoreEntryReader(
                        name: 'insertProvidersReader',
                        dimensionsStore: dimensionsStore,
                        dimensionKey: PROVIDER_DIMENSION_KEY))
                .processor(filterOutInsertedEntriesProcessor)
                .writer(insertProvidersWriter)
                .listener(logCountsStepListener())
                .build()

        def insertPatientsStep = steps.get('insertPatients')
                .chunk(insertDimensionsChunkSize)
                .reader(new DimensionsStoreEntryReader(
                        name: 'insertPatientsReader',
                        dimensionsStore: dimensionsStore,
                        dimensionKey: PATIENT_DIMENSION_KEY))
                .processor(filterOutInsertedEntriesProcessor)
                .writer(insertPatientsWriter)
                .listener(logCountsStepListener())
                .build()

        def insertVisitsStep = steps.get('insertVisits')
                .chunk(insertDimensionsChunkSize)
                .reader(new DimensionsStoreEntryReader(
                        name: 'insertVisitsReader',
                        dimensionsStore: dimensionsStore,
                        dimensionKey: VISITS_DIMENSION_KEY))
                .processor(filterOutInsertedEntriesProcessor)
                .writer(insertVisitsWriter)
                .listener(logCountsStepListener())
                .build()

        parallelFlowOf(
                'insertDimensionObjects',
                insertProvidersStep,
                insertPatientsStep,
                insertVisitsStep)
    }

    /*
     * 8. Second pass
     */
    @Bean
    @JobScope // has to be bound late because of the call to the mapping store
    Step secondPass(I2b2MappingStore mappingStore,
                    I2b2ObservationFactWriter observationFactWriter,
                    UpdatePatientsWriter updatePatientsWriter,
                    UpdateProvidersWriter updateProvidersWriter,
                    UpdateVisitsWriter updateVisitsWriter) {
        def reader = new MultiResourceItemReader<I2b2SecondPassRow>(
                strict: true,
                resources: mappingStore.allEntries*.fileResource as Set,
                delegate: secondPassInnerReader())

        steps.get('secondPass')
                .chunk(secondPassChunkSize)
                .reader(reader)
                .writer(new CompositeItemWriter(delegates: [
                        observationFactWriter,
                        updatePatientsWriter,
                        updateProvidersWriter,
                        updateVisitsWriter,
                ]))
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScope
    I2b2SecondPassInnerReader secondPassInnerReader() {
        new I2b2SecondPassInnerReader(delegate:
                tsvFileReader(
                        linesToSkip: 1,
                        columnNames: 'auto',
                        allowMissingTrailingColumns: false,
                        null))
    }

    protected void configure(SequenceReserver sequenceReserver) {
        sequenceReserver.defaultBlockSize = 500
    }
}
