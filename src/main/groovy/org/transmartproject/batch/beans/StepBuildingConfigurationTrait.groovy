package org.transmartproject.batch.beans

import groovy.transform.TypeChecked
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecutionListener
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
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.transmartproject.batch.batchartifacts.HeaderSavingLineCallbackHandler
import org.transmartproject.batch.batchartifacts.JobContextAwareTaskExecutor
import org.transmartproject.batch.batchartifacts.LineOfErrorDetectionListener
import org.transmartproject.batch.batchartifacts.LogCountsStepListener
import org.transmartproject.batch.batchartifacts.MessageResolverSpringValidator
import org.transmartproject.batch.batchartifacts.ProgressWriteListener
import org.transmartproject.batch.clinical.facts.ClinicalDataRow
import org.transmartproject.batch.facts.ClinicalFactsRowSet

import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Trait with auxiliary methods for building steps.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
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
        CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> result =
                new CompositeItemProcessor<ClinicalDataRow, ClinicalFactsRowSet>()
        result.setDelegates(processors.toList())
        result
    }

    static Step wrapStepWithName(final String name,
                                 final Step step) {
        /* bad hack to avoid getName() being called on a scoped proxy */
        new OverriddenNameStep(step: step, newName: name,)
    }

    @TypeChecked
    <T> ItemStreamReader<T> tsvFileReader(Map<String, Object> options,
                                          Resource resource) {
        /* options:
         * strict: true|false (default: yes)
         * saveState: true|false (default: yes)
         * beanClass: JavaBean class
         *            (optional, o/wise use PassThroughFieldSetMapper)
         * columnNames: names of columns, required if mapping into bean
         *              properties (List<String> or String[])). Can be
         *              'auto' to read names from header (linesToSkip
         *              must be 1 then)
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

        DelimitedLineTokenizer tokenizer =  new DelimitedLineTokenizer(
                names: ((options.columnNames && options.columnNames != 'auto') ?
                        options.columnNames : []) as String[],
                delimiter: DELIMITER_TAB,
        )

        /* if columnNames is 'auto', read the first line to determine
         * the identity of the columns */
        if (options.columnNames == 'auto') {
            LineCallbackHandler originalLch = lch
            lch = { String line ->
                if (originalLch) {
                    originalLch.handleLine line
                }
                tokenizer.names = new DelimitedLineTokenizer(DELIMITER_TAB)
                        .tokenize(line).values
            } as LineCallbackHandler
        }

        if (linesToSkip == 0 && lch) {
            throw new IllegalArgumentException(
                    'Cannot look at header is there are no lines to skip')
        }

        new FlatFileItemReader<T>(
                lineMapper: new DefaultLineMapper<T>(
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

    Validator adaptValidator(
            org.springframework.validation.Validator springValidator) {
        new MessageResolverSpringValidator(springValidator, validationMessageSource())
    }
}
