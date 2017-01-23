package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.transmartproject.batch.AppConfig

import javax.sql.DataSource

/**
 * Entry point for creating the ts_batch schema with the spring-batch tables
 * inside.
 */
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

            addResourceIfExists populator,
                    "classpath:/org/transmartproject/batch/pre-schema-${ref}.sql"

            populator.addScript(resourceLoader.getResource(
                    "classpath:/org/springframework/batch/core/schema-${ref}.sql"))

            addResourceIfExists populator,
                    "classpath:/org/transmartproject/batch/post-schema-${ref}.sql"

            populator
        }

        private ResourceDatabasePopulator getStatementPerFilePopulator() throws Exception {
            def populator = new ResourceDatabasePopulator()
            def ref = getBatchSchemaRef(env.getProperty('batch.jdbc.driver'))

            addResourceIfExists populator,
                    "classpath:/org/transmartproject/batch/post-schema-${ref}-truncator.sql"

            populator.separator = ScriptUtils.EOF_STATEMENT_SEPARATOR
            populator
        }

        void execute() {
            DatabasePopulatorUtils.execute populator, dataSource
            DatabasePopulatorUtils.execute statementPerFilePopulator, dataSource
        }

        private addResourceIfExists(ResourceDatabasePopulator populator, String script) {
            def resource = resourceLoader.getResource(script)
            if (resource.exists()) {
                populator.addScript(resource)
            }
        }

        static String getBatchSchemaRef(String driverClassname) {
            switch (driverClassname) {
                case 'org.postgresql.Driver':
                    return 'postgresql'
                case 'oracle.jdbc.driver.OracleDriver':
                    return 'oracle10g'
                case 'org.h2.Driver':
                    return 'h2'
                default:
                    throw new UnsupportedOperationException("Not supported: $driverClassname")
            }
        }
    }
}
