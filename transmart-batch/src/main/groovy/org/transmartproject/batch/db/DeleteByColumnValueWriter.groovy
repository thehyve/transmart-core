package org.transmartproject.batch.db

import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.Assert

import javax.annotation.PostConstruct

/**
 * {@link ItemWriter} that receives a chunk of items and issues a delete
 * statement for each based on a configured table and column.
 */
@Slf4j
class DeleteByColumnValueWriter<T> implements ItemWriter<T>, BeanNameAware {

    String table
    String column

    // plural, for logging only
    String entityName = 'items'

    String beanName

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @PostConstruct
    void validate() {
        Assert.notEmpty(table, 'table must be set')
        Assert.notEmpty(column, 'column must be set')
    }

    @Override
    void write(List<? extends T> items) throws Exception {
        List<Integer> affected = jdbcTemplate.batchUpdate("""
                DELETE FROM $table
                WHERE $column = :value""",
                items.collect {
                    ImmutableMap.of('value', it)
                } as Map[]) as List

        def total = affected.sum()

        if (log.debugEnabled) {
            if (!total) {
                log.debug("$beanName: no rows deleted for $entityName: $items")
            } else {
                log.debug("$beanName: number of $entityName deleted this chunk: $total")
            }
        }
    }
}
