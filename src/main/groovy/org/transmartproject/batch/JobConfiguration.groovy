package org.transmartproject.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.model.ColumnMapping
import org.transmartproject.batch.model.WordMapping
import org.transmartproject.batch.tasklet.ReadFileTasklet

@Configuration
@Import(TransmartAppConfig.class)
@ComponentScan("org.transmartproject.batch")
class JobConfiguration {

    @Autowired
    private JobBuilderFactory jobs

    @Autowired
    private StepBuilderFactory steps

    /*
    @Bean
    SimpleFlow mainFlow() {
        SimpleFlow splitFlow =
                new FlowBuilder<SimpleFlow>('Split flow')
                        .split(new SimpleAsyncTaskExecutor())
                        .add(new FlowBuilder<SimpleFlow>().start(readColumnMappingsStep()).build())
                        //.add(new FlowBuilder<SimpleFlow>().start(readWordMappingsStep()).build())
                        //.add(columnMappingFlow(), wordMappingFlow())
                        .build()
        //new FlowBuilder<>()

        splitFlow
    }
    */

    @Bean
    Step readColumnMappingsStep() {
        steps.get('readColumnMappingsStep')
                .tasklet(new ReadFileTasklet(pathParameter: JobParameters.COLUMN_MAP_FILE, reader: ColumnMapping.READER))
                .build()
    }

    @Bean
    Step readWordMappingsStep() {
        steps.get('readWordMappingsStep')
                .tasklet(new ReadFileTasklet(pathParameter: JobParameters.WORD_MAP_FILE, reader: WordMapping.READER))
                .build()
    }

    @Bean
    Job job() {
        jobs.get('job')
                //.start(mainFlow()) //@todo make this parallel
                .start(readColumnMappingsStep())
                .next(readWordMappingsStep())
                .build()
    }


}
