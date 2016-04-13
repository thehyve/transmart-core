package org.transmartproject.batch.highdim.metabolomics.data

import groovy.util.logging.Slf4j
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataStepsConfig

/**
 * Spring context for metabolomics data loading steps.
 */
@Configuration
@ComponentScan
@Slf4j
class MetabolomicsDataStepsConfig extends AbstractTypicalHdDataStepsConfig {

    @Autowired
    DatabaseImplementationClassPicker picker

    @Override
    @Bean
    ItemWriter getDeleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.METAB_DATA,
                column: 'assay_id',
                entityName: 'metabolomics data points')
    }

    @Bean
    Step partitionDataTable() {
        Tasklet noImplementationTasklet = new Tasklet() {
            @Override
            RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                log.info('Data table partition is not supported by this data type.')
            }
        }

        steps.get('partitionDataTable')
                .tasklet(noImplementationTasklet)
                .build()

    }

    @Override
    @Bean
    @JobScope
    MetabolomicsDataWriter getDataWriter() {
        new MetabolomicsDataWriter()
    }
}
