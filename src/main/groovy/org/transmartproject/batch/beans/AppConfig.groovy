package org.transmartproject.batch.beans

import com.jolbox.bonecp.BoneCPDataSource
import org.springframework.batch.core.JobParametersIncrementer
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.scope.JobScope
import org.springframework.batch.core.scope.StepScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.transmartproject.batch.db.PerDbTypeRunner
import org.transmartproject.batch.support.DefaultJobIncrementer

import javax.sql.DataSource

/**
 * Base Spring configuration for
 */
@Configuration
@EnableBatchProcessing
@PropertySource('${propertySource:file:./batchdb.properties}')
class AppConfig {

    @Autowired
    private Environment env

    @Bean(destroyMethod = "close")
    DataSource dataSource() {
        new BoneCPDataSource(
                driverClass: env.getProperty('batch.jdbc.driver'),
                jdbcUrl: env.getProperty('batch.jdbc.url'),
                username: env.getProperty('batch.jdbc.user'),
                password: env.getProperty('batch.jdbc.password')).with {
            config.initSQL = perDbTypeRunner().run([
                    postgresql: { ->
                        'SET search_path = ts_batch'
                    }
            ])
            it
        }
    }

    @Bean
    PerDbTypeRunner perDbTypeRunner() {
        new PerDbTypeRunner()
    }

    @Bean
    JobParametersIncrementer jobParametersIncrementer() {
        new DefaultJobIncrementer()
    }

    /* override beans to fix warnings due to them not being static in
     * org.springframework.batch.core.configuration.annotation.AbstractBatchConfiguration.ScopeConfiguration */
    @Bean
    static StepScope stepScope() {
        new StepScope().with {
            autoProxy = false
            it
        }
    }

    @Bean
    static JobScope jobScope() {
        new JobScope().with {
            autoProxy = false
            it
        }
    }
}
