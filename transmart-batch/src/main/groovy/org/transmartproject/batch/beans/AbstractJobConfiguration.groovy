package org.transmartproject.batch.beans

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.batchartifacts.BetterExitMessageJobExecutionListener
import org.transmartproject.batch.batchartifacts.DefaultJobIncrementer
import org.transmartproject.batch.db.*
import org.transmartproject.batch.db.oracle.OracleSequenceReserver
import org.transmartproject.batch.db.postgres.PostgresSequenceReserver

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
        'org.transmartproject.batch.secureobject',
        'org.transmartproject.batch.biodata',
])
abstract class AbstractJobConfiguration implements StepBuildingConfigurationTrait {

    public static final int DEFAULT_FETCH_SIZE = 1000

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    MessageSource validationMessageSource

    @Autowired
    DatabaseImplementationClassPicker picker

    abstract Job job()

    @Bean
    static BeanFactoryPostProcessor customizeJob() {
        { ConfigurableListableBeanFactory beanFactory ->
            def beanNames = beanFactory.getBeanNamesForType(Job)
            beanNames.each { beanName ->
                beanFactory.getBeanDefinition(beanName)
                        .propertyValues.with {
                    addPropertyValue('jobExecutionListeners',
                            [new BetterExitMessageJobExecutionListener()] as JobExecutionListener[])
                    addPropertyValue('jobParametersIncrementer', new DefaultJobIncrementer())
                }
            }
        } as BeanFactoryPostProcessor
    }

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
