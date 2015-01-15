package org.transmartproject.batch.batchartifacts

import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.support.StringUtils

import javax.sql.DataSource
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Validates sizes of strings based on the sizes of columns retrieved from
 * database metadata.
 */
@Slf4j
@SuppressWarnings('JdbcConnectionReference')
class DbMetadataBasedBoundsValidator implements Validator {

    private final Map<String, ColumnSpecification> config

    private Map<String, Map> processedConfig

    Class<?> targetClass

    String nestedPath

    @Autowired
    DataSource dataSource

    @Override
    boolean supports(Class<?> clazz) {
        targetClass.isAssignableFrom clazz
    }

    DbMetadataBasedBoundsValidator(Map config, Class<?> targetClass) {
        this.targetClass = targetClass
        this.config = config
    }

    @Override
    void validate(Object target, Errors errors) {
        BeanWrapperImpl beanWrapper =
                PropertyAccessorFactory.forBeanPropertyAccess(target)

        if (nestedPath) {
            beanWrapper.nestedPath = nestedPath
        }

        processConfig()

        processedConfig.each { String property, Map spec ->
            def curValue = beanWrapper.getPropertyValue(property) as String
            if (curValue == null && !spec.nullable) {
                errors.rejectValue property, 'require',
                        [property] as Object[], null
            } else if (curValue.size() > spec.maxSize) {
                errors.rejectValue property, 'maxSizeExceeded',
                        [property, curValue.size(), spec.maxSize] as Object[],
                        null
            }
        }
    }

    private void processConfig() {
        if (processedConfig) {
            return
        }

        def c = DataSourceUtils.getConnection(dataSource)
        try {
            processedConfig = config.collectEntries { String property,
                                              ColumnSpecification spec ->
                [property, processColumnSpecification(c.metaData, spec)]
            }
        } finally {
            DataSourceUtils.releaseConnection(c, dataSource)
        }
    }

    private Map processColumnSpecification(DatabaseMetaData meta,
                                           ColumnSpecification spec) {
        // TODO: check which character oracle uses to escape the like expressions
        ResultSet rs = meta.getColumns(
                null,
                StringUtils.escapeForLike(spec.schema),
                StringUtils.escapeForLike(spec.table),
                StringUtils.escapeForLike(spec.column),)

        if (!rs.next()) {
            throw new IllegalArgumentException(
                    "No column ${spec.column} for table ${spec.schema}.${spec.table}")
        }

        def r = [
                maxSize:  rs.getInt('COLUMN_SIZE'), // in UTF-16 code units
                                                    // will have to be improved for oracle
                nullable: rs.getInt('NULLABLE') as boolean,
        ]
        log.debug("Found for column $spec constraints $r")

        r
    }

    @Immutable
    static class ColumnSpecification {
        String schema
        String table
        String column
    }

    static ColumnSpecification c(String schema, String table, String column) {
        new ColumnSpecification(schema: schema, table: table, column: column)
    }

    static ColumnSpecification c(String qualifiedTable, String column) {
        def split = qualifiedTable.split(/\./, 2)
        assert split.size() == 2
        def schema = split[0]
        def table = split[1]
        c(schema, table, column)
    }
}
