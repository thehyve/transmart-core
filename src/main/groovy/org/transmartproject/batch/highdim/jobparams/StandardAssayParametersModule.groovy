package org.transmartproject.batch.highdim.jobparams

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Specifies the  MAP_FILENAME parameters.
 */
class StandardAssayParametersModule implements ExternalJobParametersModule {

    public final static String MAP_FILENAME = 'MAP_FILENAME'

    Set<String> supportedParameters = ImmutableSet.of(
            MAP_FILENAME,
    )

    void validate(ExternalJobParametersInternalInterface ejp) {
        ejp.mandatory MAP_FILENAME
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp[MAP_FILENAME] = ejp.convertRelativePath MAP_FILENAME
    }
}
