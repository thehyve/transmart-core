package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.converter.Converter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component

/**
 * Converter to allow injecting {@link SimpleJdbcInsertConverter} objects
 * with:
 *
 * <code>
 *     @Value('i2b2demodata.patient_dimension')
 *     SimpleJdbcInsert patientDimensionInsert
 * </code>
 *
 * A bit of a hack, yes.
 */
@Component
class SimpleJdbcInsertConverter implements Converter<String, SimpleJdbcInsert> {
    @Autowired
    JdbcTemplate jdbcTemplate

    @Override
    SimpleJdbcInsert convert(String source) {
        def ret = new SimpleJdbcInsert(jdbcTemplate)

        def matcher = source =~ /\A([^.]+)\.(.+)\z/
        if (!matcher) {
            ret.withTableName(source)
        } else {
            ret.withSchemaName(matcher.group(1)).withTableName(matcher.group(2))
        }
    }
}
