package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.transmartproject.batch.startup.*

import java.security.InvalidParameterException

/**
 * Parameters for backout job.
 */
@Slf4j
class BackoutJobSpecification
        implements ExternalJobParametersModule, JobSpecification {

    public final static String INCLUDED_TYPES = 'INCLUDED_TYPES'
    public final static String EXCLUDED_TYPES = 'EXCLUDED_TYPES'

    public final static String FULL_BACKOUT_TYPE = 'full'

    final Set<String> supportedParameters = [
            INCLUDED_TYPES,
            EXCLUDED_TYPES
    ]

    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new StudyJobParametersModule(),
            this
    ]

    final Class<?> jobPath = BackoutJobConfiguration

    @Override
    void validate(ExternalJobParametersInternalInterface ejp) {
        if (ejp[INCLUDED_TYPES] && ejp[EXCLUDED_TYPES]) {
            throw new InvalidParametersFileException(
                    "You can simultaneously specify $INCLUDED_TYPES and " +
                            "$EXCLUDED_TYPES")
        }
    }

    @Override
    void munge(ExternalJobParametersInternalInterface ejp) {
        List<String> includedTypes,
                     excludedTypes

        if (ejp[INCLUDED_TYPES]) {
            includedTypes = ejp[INCLUDED_TYPES].split(/,\s+/) as List
        } else {
            includedTypes = []
        }

        if (ejp[EXCLUDED_TYPES]) {
            excludedTypes = ejp[EXCLUDED_TYPES].split(/,\s+/) as List
        } else {
            excludedTypes = []
        }

        if (excludedTypes && !(FULL_BACKOUT_TYPE in excludedTypes)) {
            log.warn "Automatically excluding backout type '$FULL_BACKOUT_TYPE'"
            excludedTypes << FULL_BACKOUT_TYPE
        }

        if (includedTypes && (FULL_BACKOUT_TYPE in includedTypes)) {
            throw new InvalidParameterException(
                    "Cannot include $FULL_BACKOUT_TYPE in $INCLUDED_TYPES " +
                            "(it implies that all study data is to be removed)")
        }

        ejp[INCLUDED_TYPES] = includedTypes.join(',')
        ejp[EXCLUDED_TYPES] = excludedTypes.join(',')
    }
}
