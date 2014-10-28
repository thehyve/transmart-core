package org.transmartproject.batch.beans

import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.JobParametersIncrementer
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.clinical.FactRowSet
import org.transmartproject.batch.db.SimpleJdbcInsertConverter
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.support.DefaultJobIncrementer
import org.transmartproject.batch.support.JobContextAwareTaskExecutor
import org.transmartproject.batch.db.SequenceReserver

import javax.sql.DataSource
import java.nio.file.Path
import java.nio.file.Paths

@ComponentScan("org.transmartproject.batch")
abstract class AbstractJobConfiguration {

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    StepBuilderFactory steps

    @Bean
    JobParametersIncrementer jobParametersIncrementer() {
        new DefaultJobIncrementer()
    }

    @JobScope
    @Bean
    SequenceReserver sequenceReserver() {
        SequenceReserver result = new SequenceReserver()
        configure(result)
        result
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource)
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            JdbcTemplate jdbcTemplate) {
        new NamedParameterJdbcTemplate(jdbcTemplate)
    }

    @Bean
    ConversionServiceFactoryBean conversionService(
            SimpleJdbcInsertConverter simpleJdbcInsertConverter) {

        new ConversionServiceFactoryBean(converters: [
                { s -> Paths.get(s) } as StringToPathConverter,
                simpleJdbcInsertConverter
        ])
    }

    protected void configure(SequenceReserver sequenceReserver) {
        sequenceReserver.defaultBlockSize = 10
    }

    Step stepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .tasklet(tasklet)
                .build()
    }

    Step stepOf(MethodClosure closure) {
        stepOf(closure.method, closure.call())
    }

    Flow flowOf(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    Flow parallelFlowOf(String name, Step step, Step ... otherSteps) {
        new FlowBuilder<SimpleFlow>(name)
                .start(step)
                //forks execution
                .split(new JobContextAwareTaskExecutor()) //need to use a tweaked executor. see https://jira.spring.io/browse/BATCH-2269
                .add(otherSteps.collect { flowOf(it) } as Flow[])
                .end()
    }

    ItemProcessor compositeOf(ItemProcessor ... processors) {
        CompositeItemProcessor<Row, FactRowSet> result = new CompositeItemProcessor<Row, FactRowSet>()
        result.setDelegates(processors.toList())
        result
    }
}

interface StringToPathConverter extends Converter<String, Path> {}
