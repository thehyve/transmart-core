package org.transmartproject.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.task.SimpleAsyncTaskExecutor
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

    @Bean
    SimpleFlow readControlFilesFlow() {
        new FlowBuilder<SimpleFlow>('readControlFilesFlow')
                .start(dummyStep()) //we always need an initial step, hence this dummy
                .split(new SimpleAsyncTaskExecutor()) //forks execution
                .add(wrap(readColumnMappingsStep()), wrap(readWordMappingsStep()))
                .end()
    }

    private Flow wrap(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    @Bean
    SimpleFlow convertToStandardFormatFlow() {
        new FlowBuilder<SimpleFlow>('convertToStandardFormatFlow')
                .start(readControlFilesFlow())
                .build()
    }


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
                .start(convertToStandardFormatFlow())
                .end()
                .build()
    }

    @Bean
    Step dummyStep() {
        Tasklet tasklet = new Tasklet() {
            @Override
            RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                return RepeatStatus.FINISHED
            }
        }
        steps.get('dummyStep').tasklet(tasklet).build()
    }

}
