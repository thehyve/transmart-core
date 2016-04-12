package org.transmartproject.batch.clinical

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.FlowJob
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.transmartproject.batch.batchartifacts.DuplicationDetectionProcessor
import org.transmartproject.batch.batchartifacts.LogCountsStepListener
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.clinical.facts.*
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.clinical.variable.ColumnMappingFileHeaderHandler
import org.transmartproject.batch.clinical.variable.ColumnMappingValidator
import org.transmartproject.batch.clinical.xtrial.GatherXtrialNodesTasklet
import org.transmartproject.batch.clinical.xtrial.XtrialMapping
import org.transmartproject.batch.clinical.xtrial.XtrialMappingWriter
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.DeleteConceptCountsTasklet
import org.transmartproject.batch.concept.InsertConceptCountsTasklet
import org.transmartproject.batch.concept.oracle.OracleInsertConceptCountsTasklet
import org.transmartproject.batch.concept.postgresql.PostgresInsertConceptCountsTasklet
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.facts.DeleteObservationFactTasklet
import org.transmartproject.batch.support.JobParameterFileResource
import org.transmartproject.batch.tag.TagsLoadJobConfiguration
import org.transmartproject.batch.patient.GatherCurrentPatientsReader
import org.transmartproject.batch.patient.PatientSet

/**
 * Spring configuration for the clinical data job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.clinical',
        'org.transmartproject.batch.concept',
        'org.transmartproject.batch.patient',
        'org.transmartproject.batch.facts'])
@Import(TagsLoadJobConfiguration)
class ClinicalDataLoadJobConfiguration extends AbstractJobConfiguration {

    public static final String JOB_NAME = 'ClinicalDataLoadJob'
    public static final int CHUNK_SIZE = 10000

    @javax.annotation.Resource
    Step tagsLoadStep

    @javax.annotation.Resource
    Tasklet insertTableAccessTasklet
    
    @javax.annotation.Resource
    Tasklet gatherCurrentConceptsTasklet

    @javax.annotation.Resource
    Tasklet createSecureStudyTasklet

    @javax.annotation.Resource
    ItemWriter<ClinicalFactsRowSet> patientDimensionTableWriter

    @javax.annotation.Resource
    ItemWriter<ClinicalFactsRowSet> conceptsTablesWriter

    @javax.annotation.Resource
    ItemWriter<ClinicalFactsRowSet> observationFactTableWriter

    @Bean(name = 'ClinicalDataLoadJob')
    @Override
    Job job() {
        /* Unfortunately the job is not restartable at this point.
         * The concepts for categorical variables that were created up to the
         * point were the job failed are not recovered on subsequent runs.
         * Ditto for the demographic values of patients.
         * This will need to be solved with either putting PatientSet and the
         * ConceptTree on the context or, better, with two passes */
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
                .with { FlowJob job -> job.restartable = false; job }
    }

    @Bean
    Flow mainFlow() {
        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readControlFilesFlow(null)) //reads control files (column map, word map, xtrial)

                // read stuff from the DB
                .next(wrapStepWithName('gatherCurrentPatients',
                        gatherCurrentPatientsStep(null, null)))
                .next(allowStartStepOf(this.&getGatherCurrentConceptsTasklet))
                .next(allowStartStepOf(this.&gatherXtrialNodesTasklet))

                // delete data we'll be replacing
                .next(stepOf(this.&deleteObservationFactTasklet))
                .next(stepOf(this.&deleteConceptCountsTasklet))

                // main data reading and insertion step (in observation_fact)
                .next(rowProcessingStep())
                .next(tagsLoadStep)

                // insertion of ancillary data
                .next(stepOf(this.&getCreateSecureStudyTasklet))         //bio_experiment, search_secure_object
                .next(stepOf(this.&getInsertTableAccessTasklet))
                .next(stepOf('insertConceptCounts',
                             insertConceptCountsTasklet(null, null)))    //patientDimensionInsert concept_counts rows
                .build()
    }

    @Bean
    Flow readControlFilesFlow(ColumnMappingValidator columnMappingValidator) {
        Step readVariablesStep = steps.get('readVariables')
                .allowStartIfComplete(true)
                .chunk(5)
                .reader(clinicalVariablesReader(null, null))
                .processor(compositeOf(
                    duplicateClinicalVariableDetector(),
                    duplicateDemographicVariablesDetector(),
                    duplicateConceptPathDetector(),
                    new ValidatingItemProcessor(adaptValidator(columnMappingValidator))))
                .writer(saveClinicalVariableList(null))
                .build()

        Step readWordMapStep = steps.get('readWordMap')
                .allowStartIfComplete(true)
                .tasklet(readWordMapTasklet())
                .build()

        Step readXtrialsStep = steps.get('readXtrialsFile')
                .allowStartIfComplete(true)
                .chunk(5)
                .reader(xtrialMappingReader())
                .writer(xtrialsFileTaskletWriter())
                .build()

        parallelFlowOf(
                'readControlFilesFlow',
                readVariablesStep,
                readWordMapStep,
                readXtrialsStep,)
    }

    @Bean
    ItemStreamReader<ClinicalVariable> clinicalVariablesReader(
            FieldSetMapper<ClinicalVariable> clinicalVariableFieldMapper,
            ColumnMappingFileHeaderHandler columnMappingFileHeaderValidatingHandler) {
        tsvFileReader(
                columnMapFileResource(),
                mapper: clinicalVariableFieldMapper,
                saveHeader: columnMappingFileHeaderValidatingHandler,
                allowMissingTrailingColumns: true,
                linesToSkip: 1,)
    }

    @Bean
    @JobScopeInterfaced
    Resource columnMapFileResource() {
        new JobParameterFileResource(parameter: ClinicalJobSpecification.COLUMN_MAP_FILE)
    }

    @Bean
    @JobScope
    DuplicationDetectionProcessor<ClinicalVariable> duplicateClinicalVariableDetector() {
        // equality of ClinicalVariables are based on file and column number
        new DuplicationDetectionProcessor<ClinicalVariable>(saveState: false)
    }

    @Bean
    @JobScope
    DuplicationDetectionProcessor<ClinicalVariable> duplicateDemographicVariablesDetector() {
        new DuplicationDetectionProcessor<ClinicalVariable>(
                saveState: false,
                calculateKey: { ClinicalVariable it -> it.demographicVariable })
    }

    @Bean
    @JobScope
    DuplicationDetectionProcessor<ClinicalVariable> duplicateConceptPathDetector() {
        new DuplicationDetectionProcessor<ClinicalVariable>(
                saveState: false,
                calculateKey: { ClinicalVariable it -> it.conceptPath })
    }

    @Bean
    @JobScope
    PutInBeanWriter<ClinicalVariable> saveClinicalVariableList(ClinicalJobContext ctx) {
        new PutInBeanWriter<ClinicalVariable>(bean: ctx.variables)
    }

    @Bean
    @JobScope
    FlatFileItemReader<XtrialMapping> xtrialMappingReader() {
        tsvFileReader(
                xtrialFileResource(),
                beanClass: XtrialMapping,
                columnNames: ['study_prefix', 'xtrial_prefix'],
                linesToSkip: 1,
                strict: false,
        )
    }

    @Bean
    @JobScope
    XtrialMappingWriter xtrialsFileTaskletWriter() {
        new XtrialMappingWriter()
    }

    @Bean
    @JobScopeInterfaced
    Resource xtrialFileResource() {
        new JobParameterFileResource(parameter: ClinicalJobSpecification.XTRIAL_FILE)
    }

    @Bean
    @JobScope
    Step gatherCurrentPatientsStep(GatherCurrentPatientsReader reader,
                                   PatientSet patientSet) {
        steps.get('gatherPatientsStep')
                .chunk(CHUNK_SIZE)
                .reader(reader)
                .writer(new PutInBeanWriter(bean: patientSet))
                .listener(new LogCountsStepListener())
                .allowStartIfComplete(true)
                .build()
    }

    @Bean
    Step rowProcessingStep() {
        steps.get('rowProcessingStep')
                .chunk(CHUNK_SIZE)
                .reader(dataRowReader()) //read data
                .processor(compositeOf(
                    wordReplaceProcessor(), //replace words, if such is configured
                    rowToFactRowSetConverter(), //converts each Row to a FactRowSet
                ))
                .writer(compositeOf(
                    patientDimensionTableWriter,
                    conceptsTablesWriter,
                    observationFactTableWriter
                ))
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet readWordMapTasklet() {
        new ReadWordMapTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet gatherXtrialNodesTasklet() {
        new GatherXtrialNodesTasklet()
    }

    @Bean
    @StepScope
    ClinicalDataRowReader dataRowReader() {
        new ClinicalDataRowReader()
    }

    @Bean
    @StepScopeInterfaced
    ItemProcessor<ClinicalDataRow, ClinicalDataRow> wordReplaceProcessor() {
        new WordReplaceItemProcessor()
    }

    @Bean
    @StepScopeInterfaced
    ItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> rowToFactRowSetConverter() {
        new ClinicalDataRowProcessor()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertConceptCountsTasklet(DatabaseImplementationClassPicker picker,
                                       @Value("#{jobParameters['TOP_NODE']}") ConceptPath topNode) {
        picker.instantiateCorrectClass(
                OracleInsertConceptCountsTasklet,
                PostgresInsertConceptCountsTasklet)
                .with { InsertConceptCountsTasklet t ->
            t.basePath = topNode
            t
        }
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet() {
        new DeleteObservationFactTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteConceptCountsTasklet(
            @Value("#{jobParameters['TOP_NODE']}") topNode) {
        new DeleteConceptCountsTasklet(basePath: new ConceptPath(topNode))
    }

    @Bean
    static BeanFactoryPostProcessor makeTagLoadNonStrict() {
        // tags are optional when we're not running the tag job
        { ConfigurableListableBeanFactory beanFactory ->
            beanFactory.getBeanDefinition('scopedTarget.tagReader')
                    .propertyValues
                    .addPropertyValue('strict', false)
        } as BeanFactoryPostProcessor
    }
}
