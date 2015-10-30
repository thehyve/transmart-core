package org.transmartproject.batch.gwas

import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.JobSpecification

/**
 * Job specification for GWAS jobs.
 */
class GwasJobSpecification implements JobSpecification {
    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new GwasParameterModule(),
    ]

    final Class<?> jobPath = GwasJobConfiguration
}
