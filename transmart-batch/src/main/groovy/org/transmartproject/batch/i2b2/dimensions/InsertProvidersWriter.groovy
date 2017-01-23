package org.transmartproject.batch.i2b2.dimensions

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.i2b2.misc.I2b2ControlColumnsHelper

/**
 * Writes providers (with no data) to provider_dimension.
 */
@Component
@JobScopeInterfaced
class InsertProvidersWriter implements ItemWriter<DimensionsStoreEntry> {

    @Value("#{jobParameters['PROVIDER_PATH']}")
    private String providerPath

    @Value('#{tables.providerDimension}')
    private SimpleJdbcInsert providerDimensionInsert

    @Autowired
    private I2b2ControlColumnsHelper i2b2ControlColumnsHelper

    @Override
    void write(List<? extends DimensionsStoreEntry> items) throws Exception {
        providerDimensionInsert.executeBatch(items.collect {
            [
                    provider_id  : it.externalId,
                    provider_path: providerPath,
                    *            : i2b2ControlColumnsHelper.controlValues,
            ]
        } as Map[])
    }
}
