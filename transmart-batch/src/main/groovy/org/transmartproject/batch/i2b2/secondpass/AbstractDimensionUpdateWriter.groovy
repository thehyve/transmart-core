package org.transmartproject.batch.i2b2.secondpass

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.transmartproject.batch.db.UpdateQueryBuilder
import org.transmartproject.batch.i2b2.misc.I2b2ControlColumnsHelper
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable

import javax.annotation.PostConstruct

import static java.sql.Types.BIGINT

/**
 * Base class for the three dimension updaters.
 */
abstract class AbstractDimensionUpdateWriter
        implements ItemWriter<I2b2SecondPassRow>, InitializingBean, BeanNameAware {

    @SuppressWarnings('LoggerWithWrongModifiers')
    protected Logger log = LoggerFactory.getLogger(this.getClass())

    private JdbcBatchItemWriter<I2b2SecondPassRow> jdbcBatchItemWriter

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    protected I2b2ControlColumnsHelper controlColumnsHelper

    @Value('#{tables.crcSchema}')
    protected String crcSchema

    protected abstract Class<? extends DimensionI2b2Variable> getEnumClass()

    protected abstract List<String> getKeys()

    protected abstract Map<String, ?> keyValuesFromRow(I2b2SecondPassRow row)

    protected abstract Map<? extends DimensionI2b2Variable, Object> dimensionValuesFromRow(I2b2SecondPassRow row)

    String beanName

    @Lazy
    List<String> columns = {
        [
                *enumClass.values()*.dimensionColumn,
                *(controlColumnsHelper.controlValues.keySet() as List),
        ]
    }()

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Map<String, ?> defaultValues = {
        def ret = [:]
        columns.each { c -> ret[c] = null }
        controlColumnsHelper.controlValues.each { c, v ->
            ret[c] = v
        }
        ret
    }()

    private MapSqlParameterSource createMapSqlParameterSource() {
        new MapSqlParameterSource(defaultValues)
    }

    @PostConstruct
    void init() {
        jdbcBatchItemWriter = new JdbcBatchItemWriter().with {
            it.jdbcTemplate = owner.jdbcTemplate
            UpdateQueryBuilder builder = new UpdateQueryBuilder(
                    table: crcSchema + '.' + enumClass.values().first().dimensionTable)
            builder.addColumns(*columns)
            builder.addKeys(*keys)
            sql = builder.toSQL()

            itemSqlParameterSourceProvider = new ItemSqlParameterSourceProvider<I2b2SecondPassRow>() {
                @Override
                SqlParameterSource createSqlParameterSource(I2b2SecondPassRow item) {
                    def ret = createMapSqlParameterSource()

                    keyValuesFromRow(item).each { k, value ->
                        ret.addValue k, value
                    }

                    dimensionValuesFromRow(item).each { var, value ->
                        def name = var.dimensionColumn
                        def valueToAdd = value

                        if (valueToAdd instanceof BigInteger) {
                            ret.addValue name, valueToAdd, BIGINT
                        } else {
                            ret.addValue name, valueToAdd
                        }
                    }

                    ret
                }
            }

            it
        }
    }

    @Override
    void write(List<? extends I2b2SecondPassRow> items) throws Exception {
        def filteredItems = items.findAll {
            !dimensionValuesFromRow(it).isEmpty()
        }
        if (filteredItems) {
            log.debug("From ${items.size()} rows, ${filteredItems.size()} " +
                    "will issue ${filteredItems.size()} updates")
            try {
                jdbcBatchItemWriter.write(filteredItems)
            } catch (EmptyResultDataAccessException e) {
                // describe in which dimension writer the problem happended
                log.error("Error in $beanName: $e.message", e)
                throw e
            }
        } else {
            log.debug("All ${items.size()} items filtered out")
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        jdbcBatchItemWriter.afterPropertiesSet()
    }
}
