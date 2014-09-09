package org.transmartproject.batch.support

import com.jolbox.bonecp.BoneCPDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component

import javax.sql.DataSource

/**
 *
 */
@Component
class ConfigHelper {

    @Autowired
    private Environment env

    @Autowired
    private ResourceLoader resourceLoader

    DataSource getDataSource(String name) {
        new BoneCPDataSource(
                driverClass: env.getProperty("${name}.jdbc.driver"),
                jdbcUrl: env.getProperty("${name}.jdbc.url"),
                username: env.getProperty("${name}.jdbc.user"),
                password: env.getProperty("${name}.jdbc.password"))

    }

    void populate(DataSource ds, String name) {
        // initialize database
        def populator = new ResourceDatabasePopulator()
        populator.addScript(resourceLoader.getResource(env.getProperty("${name}.schema.script")))
        DatabasePopulatorUtils.execute(populator, ds)
    }

}
