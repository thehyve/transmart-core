package org.transmartproject.batch.beans

import groovy.transform.TypeChecked
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersIncrementer
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.batch.item.validator.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.convert.converter.Converter
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.batchartifacts.*
import org.transmartproject.batch.clinical.facts.ClinicalDataRow
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.db.*

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
        'org.transmartproject.batch.support',
])
abstract class AbstractJobConfiguration {

    public static final int DEFAULT_FETCH_SIZE = 1000

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    StepBuilderFactory steps

    @Autowired
    JobParametersIncrementer jobParametersIncrementer

    abstract Job job()

    @JobScope
    @Bean
    SequenceReserver sequenceReserver(DatabaseImplementationClassPicker picker) {
        SequenceReserver result = picker.instantiateCorrectClass(
                OracleSequenceReserver,
                PostgresSequenceReserver)
        configure(result)
        result
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource).with {
            fetchSize = DEFAULT_FETCH_SIZE
            it
        }
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

    final protected Step stepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .tasklet(tasklet)
                .listener(showCountStepListener())
                .build()
    }

    final protected Step allowStartStepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .allowStartIfComplete(true)
                .tasklet(tasklet)
                .listener(showCountStepListener())
                .build()
    }

    final protected Step stepOf(MethodClosure closure) {
        stepOf(closure.method - ~/^get/, closure.call())
    }

    final protected Step allowStartStepOf(MethodClosure closure) {
        allowStartStepOf(closure.method - ~/^get/, closure.call())
    }

    @Bean
    StepExecutionListener showCountStepListener() {
        new LogCountsStepListener()
    }

    Flow flowOf(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

    Flow parallelFlowOf(String name, Step step, Step... otherSteps) {
        new FlowBuilder<SimpleFlow>(name)
                .start(step)
                //forks execution
                //need to use a tweaked executor. see https://jira.spring.io/browse/BATCH-2269
                .split(new JobContextAwareTaskExecutor())
                .add(otherSteps.collect { flowOf(it) } as Flow[])
                .end()
    }

    ItemProcessor compositeOf(ItemProcessor... processors) {
        CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> result =
                new CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet>()
        result.setDelegates(processors.toList())
        result
    }

    @Bean
    @StepScope
    HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler() {
        new HeaderSavingLineCallbackHandler()
    }

    @Bean
    @StepScope
    ProgressWriteListener progressWriteListener() {
        new ProgressWriteListener()
    }

    @Bean
    @StepScope
    LineOfErrorDetectionListener lineOfErrorDetectionListener() {
        new LineOfErrorDetectionListener()
    }

    @TypeChecked
    protected
    <T> ItemStreamReader<T> tsvFileReader(Map<String, Object> options,
                                    Resource resource) {
        /* options:
         * strict: true|false (default: yes)
         * saveState: true|false (default: yes)
         * beanClass: JavaBean class
         *            (optional, o/wise use PassThroughFieldSetMapper)
         * columnNames: names of columns, required if mapping into bean
         *              properties (List<String> or String[]))
         * linesToSkip: <int> (default: 0)
         * saveHeader: true|false|LineCallbackHandler object (default: false)
         *
         */
        def strict = options.containsKey('strict') ?
                (boolean) options.strict : true
        def saveHeader = options.containsKey('saveHeader') ?
                options.saveHeader : false
        int linesToSkip = options.containsKey('linesToSkip') ?
                (options.linesToSkip as int) : 0

        if (linesToSkip == 0 && saveHeader) {
            throw new IllegalArgumentException(
                    'Cannot save header if there are no lines to skip')
        }

        LineCallbackHandler lch = null
        if (saveHeader) {
            if (saveHeader instanceof LineCallbackHandler) {
                lch = (LineCallbackHandler) saveHeader
            } else {
                lch = headerSavingLineCallbackHandler()
            }
        }

        FieldSetMapper<T> mapper
        if (options.beanClass) {
            mapper = new BeanWrapperFieldSetMapper<T>(
                    targetType: (Class<? extends T>) options.beanClass)
        } else {
            mapper = (FieldSetMapper<T>) new PassThroughFieldSetMapper()
        }
        new FlatFileItemReader<T>(
                lineMapper: new DefaultLineMapper<T>(
                        lineTokenizer: new DelimitedLineTokenizer(
                                names: (options.columnNames ?: []) as String[],
                                delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                        ),
                        fieldSetMapper: mapper,
                ),

                linesToSkip: linesToSkip,
                skippedLinesCallback: lch,

                recordSeparatorPolicy: new DefaultRecordSeparatorPolicy(),
                resource: resource,
                strict: strict,
                saveState: options.containsKey('saveState') ?
                        (boolean) options.saveState : true,
        )
    }

    @Bean
    MessageSource validationMessageSource() {
        new ResourceBundleMessageSource(
                basename: 'org.transmartproject.batch.validation_messages',
                defaultEncoding: 'UTF-8')
    }


    static protected final Step wrapStepWithName(final String name,
                                                 final Step step) {
        /* bad hack to avoid getName() being called on a scoped proxy */
        new OverriddenNameStep(step: step, newName: name,)
    }

    protected Validator adaptValidator(org.springframework.validation.Validator springValidator) {
        new MessageResolverSpringValidator(springValidator, validationMessageSource())
    }

}

/* needed so the runtime can know the generic types */
interface StringToPathConverter extends Converter<String, Path> {}

class OverriddenNameStep implements Step {
    @Delegate
    Step step

    String newName

    @Override
    String getName() {
        newName
    }
}
