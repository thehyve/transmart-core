package org.transmartproject.batch.junit

import groovy.transform.TypeChecked
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.BeansException
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.beans.factory.support.ManagedList
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.PriorityOrdered
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.Assert
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.batchartifacts.HeaderSavingLineCallbackHandler
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.OracleTableTruncator
import org.transmartproject.batch.db.PostgresTableTruncator
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.preparation.TsvFieldSetJdbcBatchItemWriter

import javax.annotation.PostConstruct
import javax.sql.DataSource

/**
 * Base context configuration for {@link LoadTablesRule}.
 */
@Configuration
@Import(AppConfig)
@TypeChecked
class LoadTablesConfiguration implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    public static final int CHUNK_SIZE = 100

    Map<String /* table */, Resource /* file */> tableFileMap

    @PostConstruct
    void init() {
        Assert.notNull(tableFileMap)
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource)
    }

    final int order = 0

    /**
     * We need to dynamically create beans (two for each table whose data we're
     * loading).
     */
    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<String> loadTableStepBeanNames = []

        // put these definitions here. A @Bean and @StepScoped annotated method
        // didn't work when I first tried (probably postprocessing/instantiation
        // order issues) and it's not worth the effort troubleshooting
        def headerHandlerSingletonBeanName = 'headerSavingCallbackHandler'
        def headerHandlerStepScopedBeanName = 'target.headerSavingCallbackHandler'
        registry.registerBeanDefinition(headerHandlerSingletonBeanName,
                new GenericBeanDefinition(
                        beanClass: ScopedProxyFactoryBean,
                        propertyValues: new MutablePropertyValues(
                                targetBeanName: headerHandlerStepScopedBeanName)))
        registry.registerBeanDefinition(headerHandlerStepScopedBeanName,
                new GenericBeanDefinition(
                        scope: 'step',
                        autowireCandidate: false,
                        beanClass: HeaderSavingLineCallbackHandler))

        tableFileMap.each { String table, Resource file ->
            String writerSingletonBeanName = "${table}Writer"
            String writerStepScopedBeanName = "target.${table}Writer"

            def writerSingletonDefinition = new GenericBeanDefinition(
                    beanClass: ScopedProxyFactoryBean,
                    propertyValues: new MutablePropertyValues(
                            targetBeanName: writerStepScopedBeanName)
            )
            def writerStepScopeDefinition = new GenericBeanDefinition(
                    scope: 'step',
                    autowireCandidate: false,
                    beanClass: TsvFieldSetJdbcBatchItemWriter,
                    propertyValues: new MutablePropertyValues(
                            table: table))

            String loadTableStepBeanName = "load_${table}"
            def loadTableStepDefinition = new GenericBeanDefinition(
                    beanClass: TableLoadStepFactory,
                    propertyValues: new MutablePropertyValues(
                            resource: file,
                            writer: new RuntimeBeanReference(writerSingletonBeanName)))

            registry.registerBeanDefinition(writerStepScopedBeanName,
                    writerStepScopeDefinition)

            registry.registerBeanDefinition(writerSingletonBeanName,
                    writerSingletonDefinition)

            registry.registerBeanDefinition(loadTableStepBeanName,
                    loadTableStepDefinition)

            loadTableStepBeanNames << loadTableStepBeanName.toString()
        }

        def stepBeanReferences = new ManagedList<RuntimeBeanReference>()
        stepBeanReferences.addAll(
                loadTableStepBeanNames.collect {
                    new RuntimeBeanReference(it)
                })

        def fillTablesFlowDefinition = new GenericBeanDefinition(
                beanClass: FillTablesFlowFactory,
                propertyValues: new MutablePropertyValues(
                        stepBeans: stepBeanReferences))
        registry.registerBeanDefinition('fillTablesFlow', fillTablesFlowDefinition)
    }

    static class TableLoadStepFactory implements FactoryBean<Step>, BeanNameAware {
        @Autowired
        StepBuilderFactory steps

        @Autowired
        HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler

        Resource resource

        ItemWriter<FieldSet> writer

        final boolean singleton = false

        final Class<?> objectType = Step

        String beanName

        @Override
        Step getObject() throws Exception {
            steps.get(beanName)
                    .chunk(CHUNK_SIZE)
                    .reader(tsvFileReader(resource))
                    .writer(writer)
                    .build()
        }

        private ItemStreamReader<FieldSet> tsvFileReader(Resource resource) {
            new FlatFileItemReader<FieldSet>(
                    lineMapper: new DefaultLineMapper<FieldSet>(
                            lineTokenizer: new DelimitedLineTokenizer(
                                    delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                            ),
                            fieldSetMapper: new PassThroughFieldSetMapper()
                    ),

                    linesToSkip: 1,
                    skippedLinesCallback: headerSavingLineCallbackHandler,

                    resource: resource,
                    strict: true,
            )
        }
    }

    static class FillTablesFlowFactory implements FactoryBean<Flow> {

        List<Step> stepBeans

        final boolean singleton = false

        final Class<?> objectType = Flow

        @Override
        Flow getObject() throws Exception {
            def fb = new FlowBuilder<SimpleFlow>('fillTablesFlow')
                    .start(stepBeans[0])
            for (int i = 1; i < stepBeans.size(); i++) {
                fb.next(stepBeans[i])
            }
            fb.build()
        }
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // purposefully left empty
        // this callback is invoked later and it's not meant to add new beans
    }

    @Bean
    Job fillTablesJob(Flow fillTablesFlow, JobBuilderFactory jobs) {
        jobs.get('fillTablesJob')
                .start(fillTablesFlow)
                .end()
                .build()
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            JdbcTemplate jdbcTemplate) {
        new NamedParameterJdbcTemplate(jdbcTemplate)
    }

    @Bean
    DatabaseImplementationClassPicker databasePicker() {
        new DatabaseImplementationClassPicker()
    }

    @Bean
    TableTruncator tableTruncator(DatabaseImplementationClassPicker databasePicker) {
        databasePicker.instantiateCorrectClass(PostgresTableTruncator, OracleTableTruncator)
    }
}
