package org.transmartproject.batch.highdim.metabolomics.platform.writers

import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.ItemPreparedStatementSetter
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.highdim.platform.Platform

/**
 * Superclass for metabolomics writers.
 */
abstract class AbstractMetabolomicsWriter<T>
        implements ItemWriter<T>, InitializingBean {

    abstract String getSql()

    abstract Closure getPreparedStatementSetter()

    @Delegate(includeTypes = ItemWriter)
    JdbcBatchItemWriter<T> delegate = new JdbcBatchItemWriter<>()

    @Autowired
    Platform platform

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    void afterPropertiesSet() {
        delegate.sql = sql
        delegate.itemPreparedStatementSetter =
                preparedStatementSetter as ItemPreparedStatementSetter
        delegate.jdbcTemplate = jdbcTemplate

        delegate.afterPropertiesSet()
    }

}
