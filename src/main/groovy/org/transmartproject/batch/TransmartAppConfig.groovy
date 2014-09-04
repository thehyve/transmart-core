package org.transmartproject.batch

import com.jolbox.bonecp.BoneCPDataSource
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

import javax.sql.DataSource

@Configuration
@EnableBatchProcessing
@PropertySource("classpath:transmart.properties")
class TransmartAppConfig {

    @Autowired
    private Environment env

    @Autowired
    private ResourceLoader resourceLoader

    @Bean(destroyMethod="close")
    DataSource dataSource() {
        //spring batch infrastructure datasource
        def ds = getDatasource('batch')
        populate(ds, 'batch')
        ds
    }

    /*
    @Bean(destroyMethod="close")
    DataSource dataSource() {
        getDatasource('tm_cz')
    }

    */

    private DataSource getDatasource(String name) {
        new BoneCPDataSource(
                driverClass: env.getProperty("${name}.jdbc.driver"),
                jdbcUrl: env.getProperty("${name}.jdbc.url"),
                username: env.getProperty("${name}.jdbc.user"),
                password: env.getProperty("${name}.jdbc.password"))
    }

    private void populate(DataSource ds, String name) {
        // initialize database
        def populator = new ResourceDatabasePopulator()
        populator.addScript(resourceLoader.getResource(env.getProperty("${name}.schema.script")))
        DatabasePopulatorUtils.execute(populator, ds)
    }

}