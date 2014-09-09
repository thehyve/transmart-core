package org.transmartproject.batch.clinical

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.core.JdbcTemplate
import org.transmartproject.batch.AbstractJobConfiguration
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.support.JobContextAwareTaskExecutor
import org.transmartproject.batch.tasklet.DeleteTableTasklet

import javax.annotation.PostConstruct

@Configuration
//@Import(TransmartAppConfig.class)
//@ComponentScan("org.transmartproject.batch")
class JobConfiguration extends AbstractJobConfiguration {

    @Bean
    static org.springframework.batch.core.scope.StepScope stepScope() {
        new org.springframework.batch.core.scope.StepScope()
    }

    @Bean
    static JobScope jobScope() {
        new JobScope()
    }

    @Bean
    JdbcTemplate jdbcTemplate() {
        new JdbcTemplate(getTransmartDataSource())
    }

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
                .next(readControlFilesFlow()) //reads control files (column map, word map, etc..)
                //@todo add real data reading steps here
                .build()
    }

    @Bean
    Flow readControlFilesFlow() {

        new FlowBuilder<SimpleFlow>('readControlFilesFlow')
                .start(readColumnMappingsStep())
                //forks execution
                .split(new JobContextAwareTaskExecutor()) //need to use a tweaked executor. see https://jira.spring.io/browse/BATCH-2269
                .add(flowOf(readWordMappingsStep()), flowOf(deleteInputTableStep()))
                .end()
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
        new DeleteTableTasklet(table: 'tm_lz.lt_src_clinical_data')
    }

    @Bean
    @Scope('step')
    ItemReader<Row> dataRowReader() {
        new DataRowReader()
    }

/*

    @Bean
    Step readClinicalDataStep() {
        FlatFileItemReader<Row> reader = new FlatFileItemReader<Row>()

        steps.get('readClinicalData')
                .chunk(10)
                .reader(reader)
                .build()
    }
*/

    @PostConstruct
    void postConstruct() {

        /*
            Connection con = transmartDataSource.connection
            try {
                println con
            } finally {
                con.close()
            }
        */
    }

}
