package org.transmartproject.batch.highdim.mrna.data

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
 * Spring context for mRNA data loading job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.highdim.mrna.data',])
class MrnaDataJobConfiguration extends AbstractStandardHighDimJobConfiguration {

    public static final String JOB_NAME = 'MrnaDataLoadJob'

    @Autowired
    MrnaDataWriter mrnaDataWriter

    @Override
    @Bean(name = 'MrnaDataLoadJob')
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
                table: Tables.MRNA_ANNOTATION,
                idColumn: 'probeset_id',
                nameColumn: 'probe_id',
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
                        tableName: Tables.MRNA_DATA,
                        partitionByColumn: 'trial_name',
                        partitionByColumnValue: studyId,
                        sequence: Sequences.MRNA_PARTITION_ID,
                        primaryKey: ['assay_id', 'probeset_id'])
            case OraclePartitionTasklet:
                return new OraclePartitionTasklet(
                        tableName: Tables.MRNA_DATA,
                        partitionByColumnValue: studyId)
            default:
                return null
        }

    }

    @Override
    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.MRNA_DATA,
                column: 'assay_id',
                entityName: 'mrna data points')
    }

    @Override
    MrnaDataWriter dataPointWriter() {
        mrnaDataWriter
    }
}
