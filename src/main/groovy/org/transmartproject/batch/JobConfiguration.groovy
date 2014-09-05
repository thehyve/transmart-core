package org.transmartproject.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.transmartproject.batch.tasklet.InitClinicalJobContextTasklet
import org.transmartproject.batch.tasklet.ReadColumnMapTasklet
import org.transmartproject.batch.tasklet.ReadWordMapTasklet

@Configuration
@Import(TransmartAppConfig.class)
@ComponentScan("org.transmartproject.batch")
class JobConfiguration {

    @Autowired
    private JobBuilderFactory jobs

    @Autowired
    private StepBuilderFactory steps

    @Bean
    Flow readControlFilesFlow() {
        new FlowBuilder<SimpleFlow>('readControlFilesFlow')
                .start(readColumnMappingsStep())
                .split(new SimpleAsyncTaskExecutor()) //forks execution
                .add(wrap(readWordMappingsStep()))
                .end()
    }

    private Flow wrap(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    @Bean
    Flow convertToStandardFormatFlow() {
        new FlowBuilder<SimpleFlow>('convertToStandardFormatFlow')
                .start(initClinicalJobContextStep()) //initializes clinical job context
                .next(readControlFilesFlow()) //reads control files (column map, word map, etc..)
                //.next(gatherPatientsStep())
                .build()
    }

    @Bean
    Step gatherPatientsStep() {
        //discover which variables are necessary in the 1st pass
        //read the patient id and other variables
        //write the patient id and variables in some temporary structure
        null
    }

    @Bean
    Step readColumnMappingsStep() {
        steps.get('readColumnMappingsStep')
                .tasklet(new ReadColumnMapTasklet())
                .build()
    }

    @Bean
    Step readWordMappingsStep() {
        steps.get('readWordMappingsStep')
                .tasklet(new ReadWordMapTasklet())
                .build()
    }

    @Bean
    Job job() {
        jobs.get('job')
                .start(convertToStandardFormatFlow())
                .end()
                .build()
    }

    @Bean
    Step initClinicalJobContextStep() {
        steps.get('initClinicalJobContextStep')
                .tasklet(new InitClinicalJobContextTasklet())
                .build()
    }

}
