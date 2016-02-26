package org.transmartproject.batch.beans

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.db.*

import javax.sql.DataSource

/**
 * General purpose Spring configuration for functional tests. Includes
 * helper beans for issuing queries, count their rows and truncate tables.
 */
@Import(AppConfig)
class GenericFunctionalTestConfiguration {

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource)
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            JdbcTemplate jdbcTemplate) {
        new NamedParameterJdbcTemplate(jdbcTemplate)
    }

    @Bean
    DatabaseImplementationClassPicker databasePicker() {
        new DatabaseImplementationClassPicker()
    }

    @Bean
    TableTruncator tableTruncator(DatabaseImplementationClassPicker databasePicker) {
        databasePicker.instantiateCorrectClass(PostgresTableTruncator, OracleTableTruncator)
    }

    @Bean
    RowCounter rowCounter() {
        new RowCounter()
    }
}
