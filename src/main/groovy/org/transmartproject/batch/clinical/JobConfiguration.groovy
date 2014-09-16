package org.transmartproject.batch.clinical

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.JobScope
import org.springframework.batch.core.scope.StepScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.core.JdbcTemplate
import org.transmartproject.batch.AbstractJobConfiguration
import org.transmartproject.batch.model.Row

@Configuration
class JobConfiguration extends AbstractJobConfiguration {

    @Bean
    Job job() {
        jobs.get('job')
                .start(mainFlow())
                .end()
                .build()
    }

    @Bean
    Flow mainFlow() {
        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readControlFilesFlow()) //reads control files (column map, word map, etc..)
                .next(readCurrentEntitiesFlow())
                .next(stepOf(this.&deleteObservationFactTasklet))
                .next(stepOf(this.&deleteConceptCountsTasklet))
                .next(rowProcessingStep())
                .next(stepOf(this.&insertUpdatePatientsTasklet))
                .build()
    }

    @Bean
    Flow readCurrentEntitiesFlow() {
        new FlowBuilder<SimpleFlow>('readExistingEntitiesFlow')
            .start(stepOf(this.&gatherCurrentPatientsTasklet))
            .next(stepOf(this.&gatherCurrentConceptsTasklet))
            .end()
    }

    @Bean
    Flow readControlFilesFlow() {
        parallelFlowOf(
                'readControlFilesFlow',
                stepOf(this.&readVariablesTasklet),
                stepOf(this.&readWordMapTasklet),
        )
    }

    @Bean
    Step rowProcessingStep() {
        steps.get('rowProcessingStep')
                .chunk(5)
                .reader(dataRowReader()) //read data
                .processor(compositeOf(
                    wordReplaceProcessor(), //replace words, if such is configured
                    rowToFactRowSetConverter(), //converts each Row to a FactRowSet
                ))
                .writer(factRowSetTableWriter()) //writes the FactRowSets in lt_src_clinical_data
                .build()
    }

    @Bean
    @Scope('job')
    Tasklet readWordMapTasklet() {
        new ReadWordMapTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet readVariablesTasklet() {
        new ReadVariablesTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet gatherCurrentPatientsTasklet() {
        new GatherCurrentPatientsTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet gatherCurrentConceptsTasklet() {
        new GatherCurrentConceptsTasklet()
    }

    @Bean
    @Scope('step')
    ItemReader<Row> dataRowReader() {
        new DataRowReader()
    }

    @Bean
    @Scope('step')
    ItemProcessor<Row,Row> wordReplaceProcessor() {
        new WordReplaceItemProcessor()
    }

    @Bean
    @Scope('step')
    ItemProcessor<Row, FactRowSet> rowToFactRowSetConverter() {
        new RowToFactRowSetConverter()
    }

    @Bean
    ItemWriter<FactRowSet> factRowSetTableWriter() {
        new ObservationFactTableWriter()
    }

    @Bean
    @Scope('step')
    Tasklet callStoredProcedureTasklet() {
        new CallI2B2LoadClinicalDataProcTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet insertUpdatePatientsTasklet() {
        new InsertUpdatePatientsTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet deleteObservationFactTasklet() {
        new DeleteObservationFactTasklet()
    }

    @Bean
    @Scope('job')
    Tasklet deleteConceptCountsTasklet() {
        new DeleteConceptCountsTasklet()
    }

    @Bean
    static StepScope stepScope() {
        new StepScope()
    }

    @Bean
    static JobScope jobScope() {
        new JobScope()
    }

    @Bean
    JdbcTemplate jdbcTemplate() {
        new JdbcTemplate(getTransmartDataSource())
    }

}
