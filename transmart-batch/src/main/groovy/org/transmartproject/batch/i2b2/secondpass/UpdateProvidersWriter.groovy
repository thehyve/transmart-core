package org.transmartproject.batch.i2b2.secondpass

import groovy.transform.CompileStatic
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable
import org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable

/**
 * Updates provider data using the provider dimension variables.
 */
@Component
@JobScope
@CompileStatic
class UpdateProvidersWriter extends AbstractDimensionUpdateWriter {

    private static final String PROVIDER_ID = 'provider_id'
    private static final String PROVIDER_PATH = 'provider_path'

    @Value("#{jobParameters['PROVIDER_PATH']}")
    private String providerPath

    private final Class<? extends DimensionI2b2Variable> enumClass =
            ProviderDimensionI2b2Variable

    // in reality provider_path is part of the PK, but we treat
    // provider_id as the PK
    private final List<String> keys = [PROVIDER_ID, PROVIDER_PATH]

    @Override
    protected Class<? extends DimensionI2b2Variable> getEnumClass() {
        enumClass
    }

    @Override
    protected List<String> getKeys() {
        keys
    }

    @Override
    protected Map<String, ?> keyValuesFromRow(I2b2SecondPassRow row) {
        [
                (PROVIDER_ID)  : row.providerId,
                (PROVIDER_PATH): providerPath,
        ]
    }

    @Override
    protected Map<ProviderDimensionI2b2Variable, Object> dimensionValuesFromRow(
            I2b2SecondPassRow row) {
        row.providerDimensionValues
    }
}
