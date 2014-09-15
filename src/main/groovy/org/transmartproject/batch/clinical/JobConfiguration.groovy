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
import org.transmartproject.batch.tasklet.DeleteTableTasklet

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
                .next(rowProcessingStep())
                .next(stepOf(this.&callStoredProcedureTasklet))
                .build()
    }

    @Bean
    Flow readCurrentEntitiesFlow() {
        new FlowBuilder<SimpleFlow>('readExistingEntitiesFlow')
            .start(stepOf(this.&gatherCurrentPatientsTasklet))
            .end()
    }

    @Bean
    Flow readControlFilesFlow() {
        parallelFlowOf(
                'readControlFilesFlow',
                stepOf(this.&readVariablesTasklet),
                stepOf(this.&readWordMapTasklet),
                stepOf(this.&deleteInputTableTasklet),
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
    Tasklet gatherCurrentPatientsTasklet() {
        new GatherCurrentPatientsTasklet()
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
        new ItemWriter<FactRowSet>() {
            @Override
            void write(List<? extends FactRowSet> items) throws Exception {
                items.each { FactRowSet set ->
                    //println "writing $set"
                    set.observationFacts.each {
                        println "writing $it"
                    }
                }
            }
        }
        //new FactRowTableWriter()
    }

    @Bean
    @Scope('step')
    Tasklet callStoredProcedureTasklet() {
        new CallI2B2LoadClinicalDataProcTasklet()
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
