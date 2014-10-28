package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.transmartproject.batch.beans.AppConfig

import javax.sql.DataSource

class BatchSchemaPopulator {

    static void main(String... args) {
        def appCtx = new AnnotationConfigApplicationContext()
        appCtx.with {
            register(AppConfig)
            register(ResourcePopulator)
            refresh()
        }

        appCtx.getBean(ResourcePopulator).execute()
    }

    static class ResourcePopulator {
        @Autowired
        ResourceLoader resourceLoader

        @Autowired
        Environment env

        @Autowired
        DataSource dataSource

        private ResourceDatabasePopulator getPopulator() throws Exception {
            def populator = new ResourceDatabasePopulator()
            def ref = getBatchSchemaRef(env.getProperty('batch.jdbc.driver'))
            populator.addScript(resourceLoader.getResource(
                    "classpath:/org/transmartproject/batch/pre-schema-${ref}.sql"))
            populator.addScript(resourceLoader.getResource(
                    "classpath:/org/springframework/batch/core/schema-${ref}.sql"))
            populator
        }

        void execute() {
            DatabasePopulatorUtils.execute populator, dataSource
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
}
