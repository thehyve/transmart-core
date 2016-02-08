package org.transmartproject.batch.highdim.rnaseq.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.CollectMinimumPositiveValueListener
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.db.oracle.OraclePartitionTasklet
import org.transmartproject.batch.highdim.beans.AbstractHighDimJobConfiguration
import org.transmartproject.batch.highdim.datastd.NegativeDataPointWarningProcessor
import org.transmartproject.batch.highdim.datastd.StandardDataValuePatientInjectionProcessor
import org.transmartproject.batch.highdim.datastd.TripleStandardDataValueLogCalculationProcessor
import org.transmartproject.batch.highdim.jobparams.StandardHighDimDataParametersModule
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.startup.StudyJobParametersModule
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring context for RNASeq data loading job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.highdim.rnaseq.data',])
class RnaSeqDataJobConfiguration extends AbstractHighDimJobConfiguration {

    public static final String JOB_NAME = 'RnaSeqDataLoadJob'

    static int dataFilePassChunkSize = 10000

    @Autowired
    RnaSeqDataWriter rnaSeqDataWriter

    @Override
    @Bean(name = 'RnaSeqDataLoadJob')
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
    }

    /***************
     * First pass *
     ***************/

    @Autowired
    RnaSeqDataValueValidator rnaSeqDataValueValidator

    @Bean
    Step firstPass() {
        CollectMinimumPositiveValueListener minPosValueColector = collectMinimumPositiveValueListener()
        TaskletStep step = steps.get('firstPass')
                .chunk(dataFilePassChunkSize)
                .reader(rnaSeqDataTsvFileReader())
                .processor(compositeOf(
                    new ValidatingItemProcessor(adaptValidator(rnaSeqDataValueValidator)),
                    warningNegativeDataPointToNaNProcessor(),
                ))
                .stream(minPosValueColector)
                .listener(minPosValueColector)
                .listener(logCountsStepListener())
                .build()

        step
    }

    @Bean
    @JobScope
    NegativeDataPointWarningProcessor warningNegativeDataPointToNaNProcessor() {
        new NegativeDataPointWarningProcessor()
    }

    @Bean
    @JobScope
    CollectMinimumPositiveValueListener collectMinimumPositiveValueListener() {
        new CollectMinimumPositiveValueListener()
    }

    /***************
     * Second pass *
     ***************/

    @Bean
    @Override
    Step secondPass() {
        TaskletStep step = steps.get('secondPass')
                .chunk(dataFilePassChunkSize)
                .reader(rnaSeqDataTsvFileReader())
                .processor(compositeOf(
                    standardDataValuePatientInjectionProcessor(),
                    tripleStandardDataValueLogCalculationProcessor()
                ))
                .writer(dataPointWriter())
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()

        step
    }

    @Bean
    StandardDataValuePatientInjectionProcessor standardDataValuePatientInjectionProcessor() {
        new StandardDataValuePatientInjectionProcessor()
    }

    @Bean
    @JobScope
    TripleStandardDataValueLogCalculationProcessor tripleStandardDataValueLogCalculationProcessor() {
        new TripleStandardDataValueLogCalculationProcessor()
    }

    /***************
     * Main *
     ***************/

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource dataFileResource() {
        new JobParameterFileResource(
                parameter: StandardHighDimDataParametersModule.DATA_FILE)
    }

    @Bean
    @JobScope
    ItemStreamReader rnaSeqDataTsvFileReader() {
        tsvFileReader(
                dataFileResource(),
                linesToSkip: 1,
                saveState: true,
                beanClass: RnaSeqDataValue,
                columnNames: ['annotation', 'sampleCode', 'readCount', 'value'])
    }

    @Override
    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.CHROMOSOMAL_REGION,
                idColumn: 'region_id',
                nameColumn: 'region_name',
        )
    }

    @Override
    @Bean
    @JobScopeInterfaced
    Tasklet partitionTasklet() {
        String studyId = JobSynchronizationManager.context
                .jobParameters[StudyJobParametersModule.STUDY_ID]
        assert studyId != null

        switch (picker.pickClass(PostgresPartitionTasklet, OraclePartitionTasklet)) {
            case PostgresPartitionTasklet:
                return new PostgresPartitionTasklet(
                        tableName: Tables.RNASEQ_DATA,
                        partitionByColumn: 'trial_name',
                        partitionByColumnValue: studyId,
                        sequence: Sequences.RNASEQ_PARTITION_ID,
                        primaryKey: ['assay_id', 'region_id'])
            case OraclePartitionTasklet:
                return new OraclePartitionTasklet(
                        tableName: Tables.RNASEQ_DATA,
                        partitionByColumnValue: studyId)
            default:
                return null
        }

    }

    @Override
    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.RNASEQ_DATA,
                column: 'assay_id')
    }

    @Override
    RnaSeqDataWriter dataPointWriter() {
        rnaSeqDataWriter
    }
}
