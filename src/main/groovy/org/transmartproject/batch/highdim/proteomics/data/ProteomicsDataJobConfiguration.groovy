package org.transmartproject.batch.highdim.proteomics.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.db.oracle.OraclePartitionTasklet
import org.transmartproject.batch.highdim.beans.AbstractStandardHighDimJobConfiguration
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader
import org.transmartproject.batch.startup.StudyJobParametersModule

/**
 * Spring context for proteomics data loading job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.highdim.proteomics.data',])
class ProteomicsDataJobConfiguration extends AbstractStandardHighDimJobConfiguration {

    public static final String JOB_NAME = 'ProteomicsDataLoadJob'

    @Autowired
    ProteomicsDataWriter proteomicsDataWriter

    @Override
    @Bean(name = 'ProteomicsDataLoadJob')
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow())
                .end()
                .build()
    }

    @Override
    @Bean
    @JobScopeInterfaced
    GatherAnnotationEntityIdsReader annotationsReader() {
        new GatherAnnotationEntityIdsReader(
                table: Tables.PROTEOMICS_ANNOTATION,
                idColumn: 'id',
                nameColumn: 'peptide',
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
                        tableName: Tables.PROTEOMICS_DATA,
                        partitionByColumn: 'trial_name',
                        partitionByColumnValue: studyId,
                        sequence: Sequences.PROTEOMICS_PARTITION_ID,
                        primaryKey: ['assay_id', 'protein_annotation_id'])
            case OraclePartitionTasklet:
                return new OraclePartitionTasklet(
                        tableName: Tables.PROTEOMICS_DATA,
                        partitionByColumnValue: studyId)
            default:
                return null
        }

    }


    @Override
    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.PROTEOMICS_DATA,
                column: 'assay_id',
                entityName: 'proteomics data points')
    }

    @Override
    ProteomicsDataWriter dataPointWriter() {
        proteomicsDataWriter
    }
}
