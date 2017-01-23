package org.transmartproject.batch.highdim.cnv.data

import groovy.util.logging.Slf4j
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.batchartifacts.MultipleItemsLineItemReader
import org.transmartproject.batch.batchartifacts.ValidationErrorMatcherBean
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.db.oracle.OraclePartitionTasklet
import org.transmartproject.batch.highdim.assays.AssayStepsConfig
import org.transmartproject.batch.highdim.assays.CurrentAssayIdsReader
import org.transmartproject.batch.highdim.datastd.FilterDataWithoutAssayMappingsItemProcessor
import org.transmartproject.batch.highdim.datastd.PatientInjectionProcessor
import org.transmartproject.batch.highdim.jobparams.StandardHighDimDataParametersModule
import org.transmartproject.batch.startup.StudyJobParametersModule
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring batch steps configuration for Cnv data upload
 */
@Configuration
@ComponentScan
@Import([DbConfig, AssayStepsConfig])
@Slf4j
class CnvDataStepsConfig implements StepBuildingConfigurationTrait {

    static int dataFilePassChunkSize = 10000

    @Autowired
    DatabaseImplementationClassPicker picker

    @Bean
    Step firstPass(ItemProcessor cnvDataValueValidatingItemProcessor) {
        TaskletStep step = steps.get('firstPass')
                .chunk(dataFilePassChunkSize)
                .reader(cnvDataTsvFileReader())
                .processor(cnvDataValueValidatingItemProcessor)
                .listener(logCountsStepListener())
                .build()

        wrapStepWithName('firstPass', step)
    }

    @Bean
    @JobScope
    ItemProcessor<CnvDataValue, CnvDataValue> cnvDataValueValidatingItemProcessor(
            CnvDataValueValidator cnvDataValueValidator,
            @Value("#{jobParameters['PROB_IS_NOT_1']}") String probIsNotOneSeverity) {

        Set<ValidationErrorMatcherBean> nonStoppingErrors = [] as Set

        if (probIsNotOneSeverity == 'WARN') {
            nonStoppingErrors.add(new ValidationErrorMatcherBean(code: 'sumOfProbabilitiesIsNotOne'))
        }

        new ValidatingItemProcessor(
                adaptValidator(
                        cnvDataValueValidator,
                        nonStoppingErrors))
    }

    @Bean
    Step deleteHdData(CurrentAssayIdsReader currentAssayIdsReader) {
        steps.get('deleteHdData')
                .chunk(100)
                .reader(currentAssayIdsReader)
                .writer(deleteCnvDataWriter())
                .build()
    }

    @Bean
    Step partitionDataTable() {
        stepOf('partitionDataTable', partitionTasklet())
    }

    @Bean
    Step secondPass(ItemWriter<CnvDataValue> cnvDataWriter,
                    ItemProcessor<CnvDataValue, CnvDataValue> compositeOfCnvSecondPassProcessors) {
        steps.get('secondPass')
                .chunk(dataFilePassChunkSize)
                .reader(cnvDataTsvFileReader())
                .processor(compositeOfCnvSecondPassProcessors)
                .writer(cnvDataWriter)
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScopeInterfaced
    ItemProcessor<CnvDataValue, CnvDataValue> compositeOfCnvSecondPassProcessors(
            @Value("#{jobParameters['SKIP_UNMAPPED_DATA']}") String skipUnmappedData) {
        def processors = []
        if (skipUnmappedData == 'Y') {
            processors << filterDataWithoutAssayMappingsItemProcessor()
        }
        processors << patientInjectionProcessor()

        compositeOf(*processors)
    }

    @Bean
    @JobScope
    FilterDataWithoutAssayMappingsItemProcessor filterDataWithoutAssayMappingsItemProcessor() {
        new FilterDataWithoutAssayMappingsItemProcessor()
    }

    @Bean
    @JobScope
    PatientInjectionProcessor patientInjectionProcessor() {
        new PatientInjectionProcessor()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource dataFileResource() {
        new JobParameterFileResource(
                parameter: StandardHighDimDataParametersModule.DATA_FILE)
    }

    @Bean
    ItemStreamReader cnvDataTsvFileReader(
            CnvDataMultipleVariablesPerSampleFieldSetMapper cnvDataMultipleSamplesFieldSetMapper) {
        new MultipleItemsLineItemReader(
                resource: dataFileResource(),
                multipleItemsFieldSetMapper: cnvDataMultipleSamplesFieldSetMapper
        )
    }

    @Bean
    @JobScopeInterfaced
    Tasklet partitionTasklet() {
        String studyId = JobSynchronizationManager.context
                .jobParameters[StudyJobParametersModule.STUDY_ID]
        assert studyId != null

        switch (picker.pickClass(PostgresPartitionTasklet, OraclePartitionTasklet)) {
            case PostgresPartitionTasklet:
                return new PostgresPartitionTasklet(
                        tableName: Tables.CNV_DATA,
                        partitionByColumn: 'trial_name',
                        partitionByColumnValue: studyId,
                        //old cnv pipeline uses seq_mrna_partition_id
                        sequence: Sequences.MRNA_PARTITION_ID,
                        primaryKey: ['assay_id', 'region_id'])
            case OraclePartitionTasklet:
                return new OraclePartitionTasklet(
                        tableName: Tables.CNV_DATA,
                        partitionByColumnValue: studyId)
            default:
                throw new IllegalStateException('No supported DBMS detected.')
        }
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCnvDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.CNV_DATA,
                column: 'assay_id')
    }

}
