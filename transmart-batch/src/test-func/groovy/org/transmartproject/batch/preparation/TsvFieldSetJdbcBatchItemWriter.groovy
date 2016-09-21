package org.transmartproject.batch.preparation

import groovy.transform.TypeChecked
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert

/**
 * Given:
 * - a table (configured in the context)
 * - a set of column names (obtained from the step context)
 *
 * Write a list of {@link FieldSet}s into the table.
 *
 * Should be step scoped.
 */
@TypeChecked
class TsvFieldSetJdbcBatchItemWriter implements ItemWriter<FieldSet> {

    String table

    // key name must be in sync with HeaderSavingLineCallbackHandler.KEY
    @Value("#{stepExecutionContext['tsv.header']}")
    List<String> columnNamesRaw

    @Lazy
    List<String> columnNames = columnNamesRaw*.toLowerCase()

    @Autowired
    JdbcTemplate jdbcTemplate

    @Lazy
    SimpleJdbcInsert jdbcInsert = generateJdbcInsert()

    @Override
    void write(final List<? extends FieldSet> items) throws Exception {
        Map<String, Object>[] arguments = items.collect { FieldSet item ->
            def index = 0
            item.values.collectEntries { String value ->
                [
                        columnNames[index++],
                        value == '' ? null : value
                ]
            }
        } as Map<String, Object>[]

        jdbcInsert.executeBatch arguments
    }

    private SimpleJdbcInsert generateJdbcInsert() {
        def splitTable = table.split(/\./)

        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(splitTable[0])
                .withTableName(splitTable[1])
                .usingColumns(columnNames as String[])
    }
}
