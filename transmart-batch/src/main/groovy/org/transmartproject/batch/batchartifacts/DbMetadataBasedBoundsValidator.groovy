package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.db.ColumnSpecification
import org.transmartproject.batch.db.DatabaseMetaDataService

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
    DatabaseMetaDataService databaseMetaDataService

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

        processedConfig = config.collectEntries { String property,
                                                  ColumnSpecification spec ->
            def columnDeclaration = databaseMetaDataService.getColumnDeclaration(spec)

            if (!columnDeclaration) {
                throw new IllegalArgumentException(
                        "No column ${spec.column} for table ${spec.schema}.${spec.table}")
            }
            [property, columnDeclaration]
        }
    }

    static ColumnSpecification c(String qualifiedTable, String column) {
        def split = qualifiedTable.split(/\./, 2)
        assert split.size() == 2
        def schema = split[0]
        def table = split[1]
        new ColumnSpecification(schema, table, column)
    }
}
