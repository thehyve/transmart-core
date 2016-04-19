package org.transmartproject.batch.db

import com.jolbox.bonecp.BoneCPDataSource
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.beans.StringToPathConverter
import org.transmartproject.batch.db.oracle.OracleSequenceReserver
import org.transmartproject.batch.db.postgres.PostgresSequenceReserver

import javax.sql.DataSource
import java.nio.file.Paths

/**
 * Database spring configuration
 */
@Configuration
@ComponentScan
@PropertySource('${propertySource:file:./batchdb.properties}')
class DbConfig {

    public static final int DEFAULT_FETCH_SIZE = 1000

    @Bean(destroyMethod = "close")
    DataSource dataSource(Environment env) {
        new BoneCPDataSource(
                driverClass: env.getProperty('batch.jdbc.driver'),
                jdbcUrl: env.getProperty('batch.jdbc.url'),
                username: env.getProperty('batch.jdbc.user'),
                password: env.getProperty('batch.jdbc.password')).with {
            def initSQL = perDbTypeRunner().run([
                    postgresql: { ->
                        'SET search_path = ts_batch'
                    },
                    oracle    : { -> },
            ])
            if (initSQL) {
                config.initSQL = initSQL
            }
            it
        }
    }

    @Bean
    PerDbTypeRunner perDbTypeRunner() {
        new PerDbTypeRunner()
    }

    @Bean
    ConversionServiceFactoryBean conversionService(
            SimpleJdbcInsertConverter simpleJdbcInsertConverter) {

        new ConversionServiceFactoryBean(converters: [
                { s -> Paths.get(s) } as StringToPathConverter,
                simpleJdbcInsertConverter
        ])
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

    @JobScope
    @Bean
    SequenceReserver sequenceReserver(DatabaseImplementationClassPicker picker) {
        SequenceReserver result = picker.instantiateCorrectClass(
                OracleSequenceReserver,
                PostgresSequenceReserver)
        result.defaultBlockSize = 10
        result
    }

    @Bean
    Integer maxVarCharLength() {
        1250
    }

    @Bean
    String isolationLevelForCreate() {
        'ISOLATION_READ_COMMITTED'
    }
}
