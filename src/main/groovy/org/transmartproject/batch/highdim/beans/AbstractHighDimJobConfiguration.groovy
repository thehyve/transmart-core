package org.transmartproject.batch.highdim.beans

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.transmartproject.batch.batchartifacts.*
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.DeleteConceptCountsTasklet
import org.transmartproject.batch.concept.InsertConceptCountsTasklet
import org.transmartproject.batch.concept.oracle.OracleInsertConceptCountsTasklet
import org.transmartproject.batch.concept.postgresql.PostgresInsertConceptCountsTasklet
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.facts.DeleteObservationFactTasklet
import org.transmartproject.batch.facts.ObservationFactTableWriter
import org.transmartproject.batch.highdim.assays.*
import org.transmartproject.batch.highdim.datastd.DataPoint
import org.transmartproject.batch.highdim.jobparams.StandardAssayParametersModule
import org.transmartproject.batch.highdim.platform.PlatformCheckTasklet
import org.transmartproject.batch.highdim.platform.PlatformJobContextKeys
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntity
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.patient.GatherCurrentPatientsReader
import org.transmartproject.batch.patient.PatientSet
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * A high dimensional job with standard assay mapping and data files.
 *
 * May need to be further broken down in the future.
 */
@ComponentScan(['org.transmartproject.batch.highdim.datastd',
                'org.transmartproject.batch.highdim.compute',
                'org.transmartproject.batch.highdim.assays',
                'org.transmartproject.batch.highdim.i2b2',
                'org.transmartproject.batch.concept',
                'org.transmartproject.batch.patient', ])
@SuppressWarnings('MethodCount')
abstract class AbstractHighDimJobConfiguration extends AbstractJobConfiguration {

    public static final int LOAD_ANNOTATION_CHUNK_SIZE = 5000
    public static final int LOAD_PATIENTS_CHUNK_SIZE = 512
    public static final int DELETE_DATA_CHUNK_SIZE = 50
    public static final int WRITE_ASSAY_CHUNK_SIZE = 50

    /********************
     * abstract  methods *
     ********************/

    abstract Step firstPass()
    abstract Step secondPass()

    abstract ItemStreamReader<AnnotationEntity> annotationsReader()
    abstract ItemWriter<Long> deleteCurrentDataWriter()
    abstract ItemWriter<? extends DataPoint> dataPointWriter()

    /********************
     * main *
     ********************/

    final protected Flow mainFlow() {
        mainFlow(null, null, null, null, null, null)
    }

    @Bean
    Flow mainFlow(Tasklet gatherCurrentConceptsTasklet,
                  Tasklet validateTopNodePreexistenceTasklet,
                  Tasklet validateHighDimensionalConceptsTasklet,
                  Step gatherCurrentPatientsStep,
                  Tasklet validatePatientIntersectionTasklet,
                  Tasklet insertConceptsTasklet) {

        def flowBuilder = new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readMappingFile(null, null, null))
                .next(checkPlatformExists())
                .on('NOT FOUND').to(stepOf('platformNotFound',
                        new FailWithMessageTasklet(
                                "Load platform \${ctx['$PlatformJobContextKeys.PLATFORM']} before")))
                .from(checkPlatformExists())

                .next(allowStartStepOf('gatherCurrentConcepts',           gatherCurrentConceptsTasklet))
                .next(allowStartStepOf('validateTopNodePreexistence',     validateTopNodePreexistenceTasklet))
                .next(allowStartStepOf('validateHighDimensionalConcepts', validateHighDimensionalConceptsTasklet))
                .next(wrapStepWithName('gatherCurrentPatients',           gatherCurrentPatientsStep))
                .next(allowStartStepOf('validatePatientIntersection',     validatePatientIntersectionTasklet))

                .next(loadAnnotationMappings())

        if (partitionTasklet()) {
            flowBuilder.next(stepOf(this.&partitionTasklet))
        }

        if (firstPass()) {
            flowBuilder
            // first pass, do validation
                    .next(wrapStepWithName('firstPass', firstPass()))
        }

        flowBuilder
                // delete current data
                .next(deleteCurrentAssayData(null))

                .next(stepOf('deleteConceptCounts', deleteConceptCountsTasklet(null)))
                .next(stepOf('deleteObservationFact', deleteObservationFactTasklet(null)))

                // write concepts; must be before writing the assays
                .next(stepOf('insertConceptsTasklet', insertConceptsTasklet))
                 // write assays
                .next(writeAssaysStep(null, null, null))
                // write pseudo-facts and their counts
                .next(writePseudoFactsStep(null))
                .next(stepOf('insertConceptCountsTasklet', insertConceptCountsTasklet(null, null)))

                // second pass, write
                .next(secondPass())

                .build()
    }

    /********************
     * mappings reading *
     ********************/

    @Bean
    Step readMappingFile(MappingsFileRowStore assayMappings,
                         MappingsFileRowValidator mappingFileRowValidator,
                         PlatformAndConceptsContextPromoterListener platformContextPromoterListener) {
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
                .processor(new ValidatingItemProcessor(
                adaptValidator(mappingFileRowValidator)))
                .writer(new PutInBeanWriter(bean: assayMappings))
                .stream(mappingFileRowValidator)
                .listener(logCountsStepListener())
                .listener(platformContextPromoterListener)
                .build()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource mappingFileResource() {
        new JobParameterFileResource(
                parameter: StandardAssayParametersModule.MAP_FILENAME)
    }

    /********************
     * Platform check *
     ********************/

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

    /*******************
     * Patient loading *
     *******************/
    @Bean
    @JobScopeInterfaced
    Step gatherCurrentPatientsStep(GatherCurrentPatientsReader reader,
                                   PatientSet patientSet) {
        steps.get('gatherPatientsStep')
                .chunk(LOAD_PATIENTS_CHUNK_SIZE)
                .reader(reader)
                .writer(new PutInBeanWriter(bean: patientSet))
                .listener(new LogCountsStepListener())
                .allowStartIfComplete(true)
                .build()
    }

    /********************
     * annotation loading *
     ********************/

    @Bean
    Step loadAnnotationMappings() {
        steps.get('loadAnnotationMappings')
                .allowStartIfComplete(true)
                .chunk(LOAD_ANNOTATION_CHUNK_SIZE)
                .reader(annotationsReader())
                .writer(new PutInBeanWriter(bean: annotationEntityMap()))
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    @JobScope
    AnnotationEntityMap annotationEntityMap() {
        new AnnotationEntityMap()
    }

    /********************************
     * partitioning step (optional) *
     ********************************/

    @SuppressWarnings(['EmptyMethodInAbstractClass'])
    Tasklet partitionTasklet() {
        null
    }

    /*******************************
     * delete assays and main data *
     *******************************/

    @Bean
    Step deleteCurrentAssayData(CurrentAssayIdsReader currentAssayIdsReader) {
        steps.get('deleteCurrentAssayData')
                .chunk(DELETE_DATA_CHUNK_SIZE)
                .reader(currentAssayIdsReader)
                .writer(new CompositeItemWriter(
                        delegates: [deleteCurrentDataWriter(), deleteCurrentAssaysWriter()]
                ))
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentAssaysWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.SUBJ_SAMPLE_MAP,
                column: 'assay_id',
                entityName: 'assay ids')
    }

    /********************
     * delete i2b2 data *
     ********************/

    @Bean
    @JobScopeInterfaced
    Tasklet deleteConceptCountsTasklet(
            // TODO: the logic here cannot survive NODE_NAME being made optional
            //       should look at TOP_NODE, mappings.allConceptPaths and find
            //       all the nodes directly under TOP_NODE that are parents of
            //       mappings.allConceptPaths
            @Value("#{jobParameters['TOP_NODE'].toString()}#{jobParameters['NODE_NAME']}")
                    ConceptPath basePath) {
        new DeleteConceptCountsTasklet(basePath: basePath)
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet(AssayMappingsRowStore mappings) {
        new DeleteObservationFactTasklet(
                highDim: true,
                basePaths: mappings.allConceptPaths)
    }

    /*****************
     * writes assays *
     *****************/

    @Bean
    Step writeAssaysStep(
            ItemWriter<Assay> assayWriter,
            ItemStreamReader<Assay> assayFromMappingFileRowReader,
            SaveAssayIdListener saveAssayIdListener) {
        steps.get('writeAssays')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(assayFromMappingFileRowReader)
                .writer(assayWriter)
                .listener(logCountsStepListener())
                .listener(saveAssayIdListener)
                .build()
    }

    /********************
     * writes i2b2 data *
     ********************/

    @Bean
    Step writePseudoFactsStep(ItemStreamReader<ClinicalFactsRowSet> dummyFactGenerator) {
        steps.get('writePseudoFactsStep')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(dummyFactGenerator)
                .writer(observationFactTableWriter())
                .listener(logCountsStepListener())
                .build()
    }

    @Bean
    @JobScope
    ObservationFactTableWriter observationFactTableWriter() {
        new ObservationFactTableWriter()
    }

    @Bean
    @JobScopeInterfaced
    // same comment as in deleteConceptCountsTasklet
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

}
