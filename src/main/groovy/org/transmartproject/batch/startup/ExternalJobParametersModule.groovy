package org.transmartproject.batch.startup

/**
 * A class supporting a certain set of parameters.
 * A trait for having a default implementation before Java 8.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
@SuppressWarnings('UnusedMethodParameter')
@SuppressWarnings('EmptyMethod')
trait ExternalJobParametersModule {

    abstract Set<String> getSupportedParameters()

    /**
     * May be used by subclasses to change the parameter values into a canonical
     * form and add/remove parameters.
     */
    void munge(ExternalJobParametersInternalInterface ejp) {}

    void validate(ExternalJobParametersInternalInterface ejp) {}
}
