package org.transmartproject.batch.beans

import com.jolbox.bonecp.BoneCPDataSource
import groovy.transform.CompileStatic
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.transmartproject.batch.db.PerDbTypeRunner
import org.transmartproject.batch.db.SimpleJdbcInsertConverter

import javax.sql.DataSource
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@EnableBatchProcessing
@PropertySource('${propertySource:file:./batchdb.properties}')
class AppConfig {

    @Autowired
    private Environment env

    @Autowired
    private ResourceLoader resourceLoader

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
}
