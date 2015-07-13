package org.transmartproject.batch.highdim.metabolomics.data

import org.springframework.batch.core.Job
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.highdim.beans.AbstractStandardHighDimJobConfiguration
import org.transmartproject.batch.highdim.platform.annotationsload.GatherAnnotationEntityIdsReader

/**
 * Spring context for mRNA data loading job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.highdim.metabolomics.data',])
class MetabolomicsDataJobConfiguration extends AbstractStandardHighDimJobConfiguration {

    public static final String JOB_NAME = 'MetabolomicsDataLoadJob'

    @Autowired
    MetabolomicsDataWriter metabolomicsDataWriter

    @Bean(name = 'MetabolomicsDataLoadJob')
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
                table: Tables.METAB_ANNOTATION,
                idColumn: 'id',
                nameColumn: 'biochemical_name',
        )
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.METAB_DATA,
                column: 'assay_id',
                entityName: 'metabolomics data points')
    }

    @Override // job scoped bean
    MetabolomicsDataWriter dataPointWriter() {
        metabolomicsDataWriter
    }
}
