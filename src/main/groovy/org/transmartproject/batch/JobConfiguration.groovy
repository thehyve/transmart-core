package org.transmartproject.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.JobScope
import org.springframework.batch.core.scope.StepScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.clinical.ReadColumnMapTasklet
import org.transmartproject.batch.clinical.ReadWordMapTasklet

@Configuration
@Import(TransmartAppConfig.class)
@ComponentScan("org.transmartproject.batch")
class JobConfiguration {

    @Autowired
    private JobBuilderFactory jobs

    @Autowired

    private StepBuilderFactory steps

    @Bean
    static StepScope stepScope() {
        new StepScope()
    }

    @Bean
    static JobScope jobScope() {
        new JobScope()
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
                //forks execution: cannot use this because job's ExecutionContext is ThreadLocal
                //.split(new SimpleAsyncTaskExecutor())
                //.add(wrap(readWordMappingsStep()))
                .next(readWordMappingsStep())
                .end()
    }

    @Bean
    Step readColumnMappingsStep() {
        steps.get('readColumnMappingsStep')
                .tasklet(readColumnMapTasklet())
                .build()
    }

    @Bean
    Step readWordMappingsStep() {
        steps.get('readWordMappingsStep')
                .tasklet(readWordMapTasklet())
                .build()
    }

    private Flow wrap(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    @Bean
    Tasklet readWordMapTasklet() {
        new ReadWordMapTasklet()
    }

    @Bean
    Tasklet readColumnMapTasklet() {
        new ReadColumnMapTasklet()
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

}
