package org.transmartproject.batch.highdim.mrna.data

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * External parameters for main mrna data.
 */
class MrnaDataExternalJobParameters extends ExternalJobParameters {

    /* just for input; will be deleted after munging */
    public final static String DATA_FILE_PREFIX = 'DATA_FILE_PREFIX' /* not sure why it's called this */

    public final static String DATA_FILE = 'DATA_FILE' /* final destination for DATA_FILE_PREFIX */
    public final static String DATA_TYPE = 'DATA_TYPE'
    public final static String LOG_BASE  = 'LOG_BASE'
    public final static String NODE_NAME = 'NODE_NAME'
    public final static String MAP_FILENAME = 'MAP_FILENAME'

    private static final String DEFAULT_NODE_NAME = 'MRNA'


    final Class jobPath = MrnaDataJobConfiguration

    final Set<String> fileParameterKeys =
            ImmutableSet.of(DATA_FILE, MAP_FILENAME)

    @Override
    void validate() throws InvalidParametersFileException {
        super.validate()

        if (this[LOG_BASE] == null) {
            this[LOG_BASE] = 2
        } else if (!this[LOG_BASE].isLong() || this[LOG_BASE] as Long != 2) {
            throw new InvalidParametersFileException("$LOG_BASE must be 2")
        }

        mandatory DATA_TYPE
        if (this[DATA_TYPE] != 'R') {
            throw new InvalidParametersFileException("$DATA_TYPE must be 'R'")
        }

        if (this[DATA_FILE_PREFIX] && this[DATA_FILE]) {
            throw new InvalidParametersFileException(
                    "Cannot set $DATA_FILE_PREFIX and $DATA_FILE simultaneously")
        }
    }

    @Override
    void doMunge() throws InvalidParametersFileException {
        super.doMunge()

        this[NODE_NAME] = this[NODE_NAME] ?: DEFAULT_NODE_NAME

        if (this[DATA_FILE_PREFIX]) {
            this[DATA_FILE] = this[DATA_FILE_PREFIX]
            params.remove(DATA_FILE_PREFIX)
        }

        [DATA_FILE, MAP_FILENAME].each { p ->
            if (this[p] == 'x') {
                this[p] == null
            } else {
                this[p] = convertRelativePath p
            }
        }
    }
}
