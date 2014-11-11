package org.transmartproject.batch.beans

import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersIncrementer
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.clinical.facts.ClinicalDataRow
import org.transmartproject.batch.clinical.facts.ClinicalFactsRowSet
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.db.SimpleJdbcInsertConverter
import org.transmartproject.batch.support.JobContextAwareTaskExecutor
import org.transmartproject.batch.support.LogCountsStepListener

import javax.sql.DataSource
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Base class for Spring context configuration classes for Jobs.
 * Each job type should have its own configuration, extended from this class.
 */
@Import(AppConfig)
@ComponentScan([
        'org.transmartproject.batch.db',
        'org.transmartproject.batch.concept',
        'org.transmartproject.batch.support',
])
abstract class AbstractJobConfiguration {

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    StepBuilderFactory steps

    @Autowired
    JobParametersIncrementer jobParametersIncrementer

    abstract Job job()

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

    final protected Step stepOf(MethodClosure closure) {
        steps.get(closure.method)
                .tasklet(closure.call())
                .listener(showCountStepListener())
                .build()
    }

    final protected Step allowStartStepOf(MethodClosure closure) {
        steps.get(closure.method)
                .allowStartIfComplete(true)
                .tasklet(closure.call())
                .listener(showCountStepListener())
                .build()
    }

    @Bean
    StepExecutionListener showCountStepListener() {
        new LogCountsStepListener()
    }

    Flow flowOf(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    Flow parallelFlowOf(String name, Step step, Step ... otherSteps) {
        new FlowBuilder<SimpleFlow>(name)
                .start(step)
                //forks execution
                //need to use a tweaked executor. see https://jira.spring.io/browse/BATCH-2269
                .split(new JobContextAwareTaskExecutor())
                .add(otherSteps.collect { flowOf(it) } as Flow[])
                .end()
    }

    ItemProcessor compositeOf(ItemProcessor ... processors) {
        CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> result =
                new CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet>()
        result.setDelegates(processors.toList())
        result
    }
}

/* needed so the runtime can know the generic types */
interface StringToPathConverter extends Converter<String, Path> {}
