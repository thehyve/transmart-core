package org.transmartproject.batch.beans

import groovy.transform.TypeChecked
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSetFactory
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.batch.item.validator.SpringValidator
import org.springframework.batch.item.validator.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.transmartproject.batch.batchartifacts.*
import org.transmartproject.batch.support.ScientificNotationFormat
import org.transmartproject.batch.support.TokenizerColumnsReplacingHeaderHandler

import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Trait with auxiliary methods for building steps.
 */
@SuppressWarnings('BracesForClassRule')
// buggy with traits
trait StepBuildingConfigurationTrait {

    @Autowired
    StepBuilderFactory steps

    // Beans

    @Bean
    @StepScope
    HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler() {
        new HeaderSavingLineCallbackHandler()
    }

    @Bean
    MessageSource validationMessageSource() {
        new ResourceBundleMessageSource(
                basename: 'org.transmartproject.batch.validation_messages',
                defaultEncoding: 'UTF-8')
    }

    @Bean
    LocalValidatorFactoryBean jsr303ValidatorRaw() {
        new LocalValidatorFactoryBean(validationMessageSource: validationMessageSource())
    }

    @Bean
    Validator jsr303Validator() {
        new SpringValidator(validator: jsr303ValidatorRaw())
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

    // end Beans

    Step stepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .tasklet(tasklet)
                .listener(new LogCountsStepListener() /* OK, no state */)
                .build()
    }

    Step allowStartStepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .allowStartIfComplete(true)
                .tasklet(tasklet)
                .listener(new LogCountsStepListener())
                .build()
    }

    Step stepOf(MethodClosure closure) {
        stepOf(closure.method - ~/^get/ - ~/Tasklet$/, closure.call())
    }

    Step allowStartStepOf(MethodClosure closure) {
        allowStartStepOf(closure.method - ~/^get/ - ~/Tasklet$/, closure.call())
    }

    @Bean
    StepExecutionListener logCountsStepListener() {
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
        new CompositeItemProcessor(delegates: processors.toList())
    }

    ItemWriter compositeOf(ItemWriter... writers) {
        new CompositeItemWriter(delegates: writers.toList())
    }

    static Step wrapStepWithName(final String name,
                                 final Step step) {
        Assert.notNull(name, 'name must be given')
        Assert.notNull(step, 'step must be given')

        /* bad hack to avoid getName() being called on a scoped proxy */
        new OverriddenNameStep(step: step, newName: name,)
    }

    private LineCallbackHandler toLineCallbackHandler(
            TokenizerColumnsReplacingHeaderHandler tokenizerColumnsReplacingHeaderHandler,
            DelimitedLineTokenizer tokenizer) {
        { String line ->
            def originalNames = new DelimitedLineTokenizer(DELIMITER_TAB)
                    .tokenize(line).values as List<String>
            tokenizer.names = tokenizerColumnsReplacingHeaderHandler
                    .handleLine(originalNames)
        } as LineCallbackHandler
    }

    /**
     * Options:
     * strict: true|false (default: yes)
     * allowMissingTrailingColumns: true|false (default: no)
     * saveState: true|false (default: yes)
     * beanClass: JavaBean class
     *            (optional, o/wise use PassThroughFieldSetMapper)
     * mapper: FieldSetMapper to use (optional, exclusive with beanClass)
     * columnNames: names of columns, required if mapping into bean
     *              properties (List<String> or String[])). Can be
     *              'auto' to read names from header (linesToSkip
     *              must be 1 then)
     * linesToSkip: <int> (default: 0)
     * emptyStringsToNull: true|false (default: false)
     * saveHeader: true|false|LineCallbackHandler|
     *             TokenizerColumnsReplacingHeaderHandler object
     *             (default: false)
     */
    @TypeChecked
    ResourceAwareItemReaderItemStream tsvFileReader(
            Map<String, Object> options, Resource resource) {
        def strict = options.containsKey('strict') ?
                (boolean) options.strict : true
        def allowMissingTrailingColumns = options
                .containsKey('allowMissingTrailingColumns') ?
                (boolean) options.allowMissingTrailingColumns : false
        def saveHeader = options.containsKey('saveHeader') ?
                options.saveHeader : false
        int linesToSkip = options.containsKey('linesToSkip') ?
                (options.linesToSkip as int) : 0

        if (!allowMissingTrailingColumns && !options.columnNames &&
                !saveHeader) {
            // TODO: change default of allowMissingTrailingColumns to true
            LoggerFactory.getLogger(StepBuildingConfigurationTrait).warn(
                    'allowMissingTrailingColumns: false will have no effect')
        }

        DelimitedLineTokenizer tokenizer = createTsvTokenizer(
                options.emptyStringsToNull,
                options.columnNames,
                allowMissingTrailingColumns)

        LineCallbackHandler lch = null
        if (saveHeader) {
            if (saveHeader instanceof LineCallbackHandler) {
                lch = (LineCallbackHandler) saveHeader
            } else if (saveHeader
                    instanceof TokenizerColumnsReplacingHeaderHandler) {
                lch = toLineCallbackHandler(
                        (TokenizerColumnsReplacingHeaderHandler) saveHeader,
                        tokenizer)
            } else {
                lch = headerSavingLineCallbackHandler()
            }
        }

        FieldSetMapper mapper
        if (options.mapper != null) {
            if (options.beanClass) {
                throw new IllegalArgumentException(
                        'Cannot specify both beanClass and mapper')
            }
            if (!(options.mapper instanceof FieldSetMapper)) {
                throw new IllegalArgumentException('Expected mapper option, if provided, to be FieldSetMapper')
            }
            mapper = (FieldSetMapper) options.mapper
        } else if (options.beanClass) {
            mapper = new BeanWrapperFieldSetMapper(
                    targetType: (Class) options.beanClass)
        } else {
            mapper = (FieldSetMapper) new PassThroughFieldSetMapper()
        }

        /* if columnNames is 'auto', read the first line to determine
         * the identity of the columns */
        if (options.columnNames == 'auto') {
            LineCallbackHandler originalLch = lch
            lch = { String line ->
                tokenizer.names = new DelimitedLineTokenizer(DELIMITER_TAB)
                        .tokenize(line).values
                // allow a chance to originalLch to still customize tokenizer
                // (see metaclass below) among whatever else it wants to do
                if (originalLch) {
                    originalLch.handleLine line
                }
            } as LineCallbackHandler
        }

        if (linesToSkip == 0 && lch) {
            throw new IllegalArgumentException(
                    'Cannot look at header is there are no lines to skip')
        }

        new FlatFileItemReader(
                lineMapper: new DefaultLineMapper(
                        lineTokenizer: tokenizer,
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

    private DelimitedLineTokenizer createTsvTokenizer(emptyStringsToNull,
                                                      columnNames,
                                                      allowMissingTrailingColumns) {
        def tokenizerClass = DelimitedLineTokenizer
        if (emptyStringsToNull) {
            tokenizerClass = EmptyStringsToNullLineTokenizer
        }

        DelimitedLineTokenizer result = tokenizerClass.newInstance(
                names: ((columnNames && columnNames != 'auto') ?
                        columnNames : []) as String[],
                delimiter: DELIMITER_TAB,
                strict: !allowMissingTrailingColumns,)

        FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory(
                numberFormat: new ScientificNotationFormat()
        )
        result.setFieldSetFactory(fieldSetFactory)

        result
    }

    Validator adaptValidator(
            org.springframework.validation.Validator springValidator) {
        adaptValidator(springValidator, [] as Set)
    }

    Validator adaptValidator(
            org.springframework.validation.Validator springValidator,
            Set<ValidationErrorMatcherBean> nonStoppingValidationErrors) {
        new MessageResolverSpringValidator(springValidator, validationMessageSource(), nonStoppingValidationErrors)
    }
}
