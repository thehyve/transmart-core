package org.transmartproject.batch.highdim.jobparams

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Specifies the NODE_NAME and MAP_FILENAME parameters.
 */
class StandardAssayParametersModule implements ExternalJobParametersModule {

    public final static String NODE_NAME = 'NODE_NAME'
    public final static String MAP_FILENAME = 'MAP_FILENAME'

    private final String defaultNodeName

    StandardAssayParametersModule(String defaultNodeName) {
        this.defaultNodeName = defaultNodeName
    }

    Set<String> supportedParameters = ImmutableSet.of(
            NODE_NAME,
            MAP_FILENAME,)

    void validate(ExternalJobParametersInternalInterface ejp) {
        ejp.mandatory MAP_FILENAME
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp[NODE_NAME] = ejp[NODE_NAME] ?: defaultNodeName

        ejp[MAP_FILENAME] = ejp.convertRelativePath MAP_FILENAME
    }
}
