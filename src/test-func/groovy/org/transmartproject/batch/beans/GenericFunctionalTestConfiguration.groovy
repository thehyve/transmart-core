package org.transmartproject.batch.beans

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator

import javax.sql.DataSource

/**
 * General purpose Spring configuration for functional tests. Includes
 * helper beans for
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
    TableTruncator tableTruncator() {
        new TableTruncator()
    }

    @Bean
    RowCounter rowCounter() {
        new RowCounter()
    }
}
