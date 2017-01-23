package org.transmartproject.batch.i2b2

import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.JobSpecification

/**
 * Parameters for I2B2 job.
 */
@TypeChecked
class I2b2JobSpecification implements JobSpecification {

    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new I2b2ParametersModule(),
    ]

    final Class<?> jobPath = I2b2JobConfiguration
}
