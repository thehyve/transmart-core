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
    public final static String SKIP_UNMAPPED_DATA = 'SKIP_UNMAPPED_DATA'

    Set<String> supportedParameters = ImmutableSet.of(
            MAP_FILENAME,
            SKIP_UNMAPPED_DATA,
    )

    void validate(ExternalJobParametersInternalInterface ejp) {
        mandatory ejp, MAP_FILENAME
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp[MAP_FILENAME] = convertRelativePath ejp, MAP_FILENAME
        mungeBoolean(ejp, SKIP_UNMAPPED_DATA, false)
    }
}
