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
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.core.JdbcTemplate
import org.transmartproject.batch.AbstractJobConfiguration
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.support.JobContextAwareTaskExecutor
import org.transmartproject.batch.tasklet.DeleteTableTasklet

@Configuration
class JobConfiguration extends AbstractJobConfiguration {

    @Bean
    Job job() {
        jobs.get('job')
                .start(convertToStandardFormatFlow())
                .end()
                .build()
    }

    @Bean
    Flow convertToStandardFormatFlow() {
        new FlowBuilder<SimpleFlow>('convertToStandardFormatFlow')
                .start(readControlFilesFlow()) //reads control files (column map, word map, etc..)
                .next(dataProcessingFlow())
                .next(callStoredProcedureStep())
                .build()
    }

    @Bean
    Flow readControlFilesFlow() {

        new FlowBuilder<SimpleFlow>('readControlFilesFlow')
                .start(readColumnMappingsStep())
                //forks execution
                .split(new JobContextAwareTaskExecutor()) //need to use a tweaked executor. see https://jira.spring.io/browse/BATCH-2269
                .add(flowOf(readWordMappingsStep()), flowOf(deleteInputTableStep()))
                //.next(readWordMappingsStep())
                //.next(deleteInputTableStep())
                .end()
    }

    @Bean
    Step rowProcessingStep() {
        steps.get('rowProcessingStep')
                .chunk(5)
                .reader(dataRowReader()) //read data
                .processor(dataProcessor())
                .writer(factRowSetTableWriter()) //writes the FactRowSets in lt_src_clinical_data
                .build()
    }

    @Bean
    ItemProcessor<Row, FactRowSet> dataProcessor() {
        List<ItemProcessor> processors = [
                wordReplaceProcessor(), //replace words, if such is configured
                rowToFactRowSetConverter(), //converts each Row to a FactRowSet
        ]
        CompositeItemProcessor<Row, FactRowSet> result = new CompositeItemProcessor<Row, FactRowSet>()
        result.setDelegates(processors)
        result
    }

    @Bean
    Step readColumnMappingsStep() {
        stepOf(this.&readVariablesTasklet)
    }

    @Bean
    Step readWordMappingsStep() {
        stepOf(this.&readWordMapTasklet)
    }

    @Bean
    Step deleteInputTableStep() {
        stepOf(this.&deleteInputTableTasklet)
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
    Tasklet deleteInputTableTasklet() {
        new DeleteTableTasklet(table: FactRowTableWriter.TABLE) //'tm_lz.lt_src_clinical_data')
    }

    @Bean
    @Scope('step')
    ItemReader<Row> dataRowReader() {
        new DataRowReader()
    }

    @Bean
    Flow dataProcessingFlow() {
        new FlowBuilder<SimpleFlow>('dataProcessingFlow')
            .start(rowProcessingStep())
            .end()
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
        new FactRowTableWriter()
    }

    @Bean
    @Scope('step')
    Tasklet callStoredProcedureTasklet() {
        new CallI2B2LoadClinicalDataProcTasklet()
    }


    @Bean
    Step callStoredProcedureStep() {
        stepOf(this.&callStoredProcedureTasklet)
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
