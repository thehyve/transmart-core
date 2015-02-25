package org.transmartproject.batch.startup

/**
 * The set of parameter modules and a configuration class.
 */
interface JobSpecification {

    /**
     * The list of job parameter modules, in the order they should be evaluated.
     */
    List<? extends ExternalJobParametersModule> getJobParametersModules()

    /**
     * The configuration class.
     */
    Class<?> getJobPath()
}

