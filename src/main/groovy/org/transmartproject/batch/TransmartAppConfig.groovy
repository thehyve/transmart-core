package org.transmartproject.batch

import org.springframework.batch.core.configuration.annotation.BatchConfigurer
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.transmartproject.batch.support.ConfigHelper

import javax.sql.DataSource

@Configuration
@EnableBatchProcessing
@PropertySource("classpath:transmart.properties")
class TransmartAppConfig {

    @Autowired
    private ConfigHelper helper

    @Primary
    @Bean(destroyMethod="close")
    DataSource batchDataSource() {
        //spring batch infrastructure datasource
        def name = 'batch'
        def ds = helper.getDataSource(name)
        helper.populate(ds, name)
        ds
    }

    @Bean(name='transmartDataSource', destroyMethod="close")
    DataSource transmartDataSource() {
        DataSource ds = helper.getDataSource('transmart')
        //ds.setLogWriter(new PrintWriter(System.out))
        ds
    }

    @Bean
    BatchConfigurer batchConfigurer() {
        new DefaultBatchConfigurer(batchDataSource())
    }

}