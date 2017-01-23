package org.transmartproject.batch.highdim.mirna.data

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
import org.springframework.context.annotation.Import
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataStepsConfig

/**
 * Spring context for miRNA data loading steps.
 */
@Configuration
@ComponentScan
@Import(DbConfig)
@Slf4j
class MirnaDataStepsConfig extends AbstractTypicalHdDataStepsConfig {

    @Autowired
    DatabaseImplementationClassPicker picker

    @Bean
    @Override
    ItemWriter getDeleteCurrentDataWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.MIRNA_DATA,
                column: 'assay_id',
                entityName: 'mirna data points')
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

    @Bean
    @Override
    @JobScope
    MirnaDataWriter getDataWriter() {
        new MirnaDataWriter()
    }
}
