package example

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(AppConfig.class)
@ComponentScan("example")
class ExampleConfiguration {

    @Autowired
    private JobBuilderFactory jobs

    @Autowired
    private StepBuilderFactory steps

    @Bean
    Job job1(Step step1) {
        jobs.get("job1")
                .start(step1)
                .build()
    }

    @Bean
    Step step1(ItemReader<Object> reader, ItemWriter<Object> writer) {
        steps.get("step1")
                .<Object, Object>chunk(1)
                .reader(reader)
                .writer(writer)
                .build()
    }
}
