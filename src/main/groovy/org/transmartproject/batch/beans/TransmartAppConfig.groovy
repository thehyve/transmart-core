package org.transmartproject.batch.beans

import com.jolbox.bonecp.BoneCPDataSource
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

import javax.sql.DataSource
import java.sql.SQLException

@Configuration
@EnableBatchProcessing
//@PropertySource("classpath:transmart.properties")
//@PropertySource("${propertySource:batchdb.properties}")
@PropertySource('${propertySource}')
class TransmartAppConfig {

    @Autowired
    private Environment env

    @Autowired
    private ResourceLoader resourceLoader


    @Bean(name='transmartDataSource', destroyMethod="close")
    DataSource transmartDataSource() {
        DataSource ds = getDataSource('transmart', true)
        ds
    }

    private DataSource getDataSource(String name, boolean initBatchSchema) {
        String driverClassname = env.getProperty("${name}.jdbc.driver")


        DataSource ds = new BoneCPDataSource(
                driverClass: driverClassname,
                jdbcUrl: env.getProperty("${name}.jdbc.url"),
                username: env.getProperty("${name}.jdbc.user"),
                password: env.getProperty("${name}.jdbc.password"))

        //@TODO remove this and assume batch tables are already created once the transmart-data is updated
        if (initBatchSchema) {
            // initialize batch database
            def ref = getBatchSchemaRef(driverClassname)
            def populator = new ResourceDatabasePopulator()
            //populator.addScript(resourceLoader.getResource("classpath:/org/springframework/batch/core/schema-drop-${ref}.sql"))
            //populator.addScript(resourceLoader.getResource("classpath:/org/springframework/batch/core/schema-${ref}.sql"))
            //DatabasePopulatorUtils.execute(populator, ds)
        }

        ds
    }

    static String getBatchSchemaRef(String driverClassname) {
        switch (driverClassname) {
            case 'org.postgresql.Driver':
                return 'postgresql'
            default:
                throw new UnsupportedOperationException("Not supported: $driverClassname")
        }
    }

}
