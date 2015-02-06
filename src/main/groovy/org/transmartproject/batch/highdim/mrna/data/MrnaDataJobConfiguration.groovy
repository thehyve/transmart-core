package org.transmartproject.batch.highdim.mrna.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.FailWithMessageTasklet
import org.transmartproject.batch.batchartifacts.FoundExitStatusChangeListener
import org.transmartproject.batch.batchartifacts.LogCountsStepListener
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.DeleteConceptCountsTasklet
import org.transmartproject.batch.concept.InsertConceptCountsTasklet
import org.transmartproject.batch.concept.oracle.OracleInsertConceptCountsTasklet
import org.transmartproject.batch.concept.postgresql.PostgresInsertConceptCountsTasklet
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.facts.DeleteObservationFactTasklet
import org.transmartproject.batch.facts.ObservationFactTableWriter
import org.transmartproject.batch.highdim.assays.Assay
import org.transmartproject.batch.highdim.assays.CurrentAssayIdsReader
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.highdim.assays.SaveAssayIdListener
import org.transmartproject.batch.highdim.mrna.data.mapping.MrnaMappingContextPromoterListener
import org.transmartproject.batch.highdim.mrna.data.mapping.MrnaMappings
import org.transmartproject.batch.highdim.mrna.data.mapping.MrnaMappingsWriter
import org.transmartproject.batch.highdim.mrna.data.pass.MeanAndVariancePromoter
import org.transmartproject.batch.highdim.mrna.data.pass.MrnaDataRowSplitterReader
import org.transmartproject.batch.highdim.mrna.data.pass.MrnaDataWriter
import org.transmartproject.batch.highdim.mrna.data.pass.MrnaStatisticsWriter
import org.transmartproject.batch.highdim.mrna.data.validation.DataFileHeaderValidator
import org.transmartproject.batch.highdim.mrna.data.validation.VisitedProbesValidatingReader
import org.transmartproject.batch.highdim.platform.PlatformCheckTasklet
import org.transmartproject.batch.highdim.platform.PlatformJobContextKeys
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.support.JobParameterFileResource
import org.transmartproject.batch.support.OnlineMeanAndVarianceCalculator

import javax.annotation.Resource

/**
 * Spring context for mRNA data loading job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.highdim.mrna.data',
                'org.transmartproject.batch.highdim.assays',
                'org.transmartproject.batch.highdim.dummyfacts',
                'org.transmartproject.batch.concept',
                'org.transmartproject.batch.patient',])
class MrnaDataJobConfiguration extends AbstractJobConfiguration {

    public static final String JOB_NAME = 'MrnaDataLoadJob'
    public static final int LOAD_ANNOTATION_CHUNK_SIZE = 5000
    public static final int DELETE_DATA_CHUNK_SIZE = 50
    public static final int WRITE_ASSAY_CHUNK_SIZE = 50
    public static final int DATA_FILE_PASS_CHUNK_SIZE = 10000

    @Resource
    Tasklet gatherCurrentConceptsTasklet

    @Resource
    Tasklet validateTopNodePreexistenceTasklet

    @Resource
    Tasklet validateHighDimensionalConceptsTasklet

    @Resource
    Tasklet gatherCurrentPatientsTasklet

    @Resource
    Tasklet validatePatientIntersectionTasklet

    @Resource
    Tasklet insertConceptsTasklet

    @Bean(name = 'MrnaDataLoadJob')
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
    }

    private Flow mainFlow() {
        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readMappingFile(null, null))
                .next(checkPlatformExists())
                .on('NOT FOUND').to(stepOf('platformNotFound',
                        new FailWithMessageTasklet(
                                "Load platform \${ctx['$PlatformJobContextKeys.PLATFORM']} before")))
                .from(checkPlatformExists())

                .next(allowStartStepOf(this.&getGatherCurrentConceptsTasklet))
                .next(allowStartStepOf(this.&getValidateTopNodePreexistenceTasklet))
                .next(allowStartStepOf(this.&getValidateHighDimensionalConceptsTasklet))
                .next(allowStartStepOf(this.&getGatherCurrentPatientsTasklet))
                .next(allowStartStepOf(this.&getValidatePatientIntersectionTasklet))
                .next(loadAnnotationMappings())

                .next(stepOf(this.&partitionTasklet))

                // first pass, calculate mean and variance as well as whatever is needed for validation
                .next(wrapStepWithName('firstPass', firstPass(null, null)))

                // delete current data
                .next(deleteCurrentAssayData(null))
                .next(stepOf(this.&deleteConceptCountsTasklet))
                .next(stepOf(this.&deleteObservationFactTasklet))

                // write concepts
                .next(stepOf(this.&getInsertConceptsTasklet))
                // write assays
                .next(writeAssaysStep(null, null, null))
                // write pseudo-facts and their counts
                .next(writePseudoFactsStep(null))
                .next(stepOf('insertConceptCountsTasklet', insertConceptCountsTasklet(null, null)))

                // second pass, write
                .next(wrapStepWithName('secondPass', secondPass(null)))

                .build()
    }

    @Bean
    Step checkPlatformExists() {
        steps.get('checkPlatformExists')
                .allowStartIfComplete(true)
                .tasklet(platformCheckTasklet())
                .listener(new FoundExitStatusChangeListener(notifyOnFound: false /* notify on not found */))
                .build()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet platformCheckTasklet() {
        new PlatformCheckTasklet()
    }

    @Bean
    Step readMappingFile(MrnaMappingsWriter mrnaMappingsWriter,
                         MrnaMappingContextPromoterListener platformContextPromoterListener) {
        def reader = tsvFileReader(
                mappingFileResource(),
                beanClass: MappingFileRow,
                columnNames: ['studyId', 'siteId', 'subjectId', 'sampleCd',
                              'platform', 'tissueType', 'attr1', 'attr2',
                              'categoryCd', 'source_cd'],
                linesToSkip: 1,
                saveState: false,)


        steps.get('readMappingFile')
                .allowStartIfComplete(true)
                .chunk(1)
                .reader(reader)
                .writer(mrnaMappingsWriter)
                .listener(new LogCountsStepListener())
                .listener(platformContextPromoterListener)
                .build()
    }

    @Bean
    Step loadAnnotationMappings() {
        steps.get('loadAnnotationMappings')
                .allowStartIfComplete(true)
                .chunk(LOAD_ANNOTATION_CHUNK_SIZE)
                .reader(annotationsReader())
                .writer(new PutInBeanWriter(
                    bean: annotationEntityMap()
                 ))
                .listener(new LogCountsStepListener())
                .build()
    }

    @Bean
    @JobScope
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.MRNA_ANNOTATION,
                idColumn: 'probeset_id',
                nameColumn: 'probe_id',
        )
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource mappingFileResource() {
        new JobParameterFileResource(parameter: MrnaDataExternalJobParameters.MAP_FILENAME)
    }

    @Bean
    Step deleteCurrentAssayData(CurrentAssayIdsReader currentAssayIdsReader) {
        steps.get('deleteCurrentAssayData')
                .chunk(DELETE_DATA_CHUNK_SIZE)
                .reader(currentAssayIdsReader)
                .writer(new CompositeItemWriter(
                    delegates: [deleteCurrentDataWriter(), deleteCurrentAssaysWriter()]
                ))
                .listener(new LogCountsStepListener())
                .build()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteConceptCountsTasklet(
            @Value("#{jobParameters['TOP_NODE'].toString()}#{jobParameters['NODE_NAME']}")
                    ConceptPath basePath) {
        new DeleteConceptCountsTasklet(basePath: basePath)
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet(MrnaMappings mappings) {
        new DeleteObservationFactTasklet(
                highDim: true,
                basePaths: mappings.allConceptPaths)
    }

    @Bean
    Step firstPass(MrnaStatisticsWriter mrnaStatisticsWriter,
                   MeanAndVariancePromoter meanAndVariancePromoter) {
        TaskletStep step = steps.get('firstPass')
                .chunk(DATA_FILE_PASS_CHUNK_SIZE)
                .reader(firstPassDataRowSplitterReader())
                .writer(mrnaStatisticsWriter)
                .listener(new LogCountsStepListener())
                .listener(progressWriteListener())
                .listener(meanAndVariancePromoter)
                .build()

        step.streams = [firstPassTsvFileReader(null),
                        visitedProbesValidatingReader()]

        step
    }

    @Bean
    @JobScope
    ItemStreamReader firstPassTsvFileReader(DataFileHeaderValidator dataFileHeaderValidator) {
        tsvFileReader(
                dataFileResource(),
                linesToSkip: 1,
                saveHeader: dataFileHeaderValidator,
                saveState: true)
    }

    @Bean
    @StepScope
    VisitedProbesValidatingReader visitedProbesValidatingReader() {
        new VisitedProbesValidatingReader(delegate: firstPassTsvFileReader(null))
    }

    @Bean
    @StepScope
    MrnaDataRowSplitterReader firstPassDataRowSplitterReader() {
        new MrnaDataRowSplitterReader(delegate: visitedProbesValidatingReader())
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource dataFileResource() {
        new JobParameterFileResource(parameter: MrnaDataExternalJobParameters.DATA_FILE)
    }

    @Bean
    @JobScope
    OnlineMeanAndVarianceCalculator onlineMeanAndVarianceCalculator() {
        new OnlineMeanAndVarianceCalculator()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet partitionTasklet() {
        String studyId = JobSynchronizationManager.context
                .jobParameters[ExternalJobParameters.STUDY_ID]
        assert studyId != null
        new PostgresPartitionTasklet(
                tableName: Tables.MRNA_DATA,
                partitionByColumn: 'trial_name',
                partitionByColumnValue: studyId,
                sequence: Sequences.MRNA_PARTITION_ID,
                indexes: [
                        ['assay_id'],
                        ['probeset_id'],
                        ['patient_id'],
                ])
    }

    @Bean
    Step writeAssaysStep(
            ItemWriter<Assay> assayWriter,
            ItemStreamReader<Assay> mrnaMappingAssayReader,
            SaveAssayIdListener saveAssayIdListener) {
        steps.get('writeAssays')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(mrnaMappingAssayReader)
                .writer(assayWriter)
                .listener(new LogCountsStepListener())
                .listener(saveAssayIdListener)
                .build()
    }

    @Bean
    Step writePseudoFactsStep(ItemStreamReader<ClinicalFactsRowSet> dummyFactGenerator) {
        steps.get('writePseudoFactsStep')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(dummyFactGenerator)
                .writer(observationFactTableWriter())
                .listener(new LogCountsStepListener())
                .build()
    }

    @Bean
    @JobScope
    ObservationFactTableWriter observationFactTableWriter() {
        new ObservationFactTableWriter()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertConceptCountsTasklet(DatabaseImplementationClassPicker picker,
                                       @Value("#{jobParameters['TOP_NODE'].toString()}#{jobParameters['NODE_NAME']}")
                                               ConceptPath basePath) {
        picker.instantiateCorrectClass(
                OracleInsertConceptCountsTasklet,
                PostgresInsertConceptCountsTasklet).with { InsertConceptCountsTasklet t ->
            t.basePath = basePath
            t
        }
    }

    @Bean
    Step secondPass(MrnaDataWriter mrnaDataWriter) {
        TaskletStep step = steps.get('secondPass')
                .chunk(DATA_FILE_PASS_CHUNK_SIZE)
                .reader(secondPassDataRowSplitterReader())
                .writer(mrnaDataWriter)
                .listener(new LogCountsStepListener())
                .listener(progressWriteListener())
                .build()

        step.streams = [secondPassTsvFileReader()]

        step
    }

    @Bean
    @JobScope
    ItemStreamReader secondPassTsvFileReader() {
        tsvFileReader(
                dataFileResource(),
                saveHeader: true,
                linesToSkip: 1,
                saveState: true)
    }

    @Bean
    @StepScope
    MrnaDataRowSplitterReader secondPassDataRowSplitterReader() {
        new MrnaDataRowSplitterReader(delegate: secondPassTsvFileReader())
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.MRNA_DATA,
                column: 'assay_id',
                entityName: 'mrna data points')
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentAssaysWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.SUBJ_SAMPLE_MAP,
                column: 'assay_id',
                entityName: 'assay ids')
    }

}
