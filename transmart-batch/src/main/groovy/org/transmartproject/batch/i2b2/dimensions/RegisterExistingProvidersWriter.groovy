package org.transmartproject.batch.i2b2.dimensions

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

import static org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable.PROVIDER_DIMENSION_KEY

/**
 * Searches the database for existing providers and writes them to the
 * {@link DimensionsStore}.
 */
@Component
@JobScope
@Slf4j
class RegisterExistingProvidersWriter implements ItemWriter<String> {

    public static final String PROVIDER_ID_COLUMN = 'provider_id'

    @Value("#{jobParameters['PROVIDER_PATH']}")
    private String providerPath

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Autowired
    private DimensionsStore dimensionsStore

    @Value('#{tables.providerDimension}')
    private String providerDimensionTable

    @Override
    void write(List<? extends String> items) throws Exception {
        Object[] params = new Object[items.size() + 1]

        items.eachWithIndex { String item, int index ->
            params[index] = item
        }
        params[-1] = providerPath

        List<Map<String, Object>> result = jdbcTemplate.queryForList """
                SELECT $PROVIDER_ID_COLUMN
                FROM ${providerDimensionTable}
                WHERE provider_id IN (${items.collect { '?' }.join ', '})
                        AND provider_path = ?""",
                params

        log.debug("Providers: from ${items.size()} items, " +
                "found ${result.size()}")

        Set<String> notSeen = Sets.newTreeSet(items)
        result.each { Map<String, Object> map ->
            log.trace("Providers: seen $map")
            dimensionsStore.syncWithDatabaseEntry(
                    PROVIDER_DIMENSION_KEY,
                    map[PROVIDER_ID_COLUMN],
                    map[PROVIDER_ID_COLUMN],
                    (String) null)

            notSeen.remove(map[PROVIDER_ID_COLUMN])
        }

        // generally not needed, but (not seen is the default state), but
        // useful if restarting the step after changing the database
        log.trace("Providers: not seen: $notSeen")
        notSeen.each {
            dimensionsStore.markAsNotInDatabase(PROVIDER_DIMENSION_KEY, it)
        }
    }
}
