package org.transmartproject.batch.highdim.jobparams

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Defines the parameters necessary for standard data file processing.
 */
class StandardHighDimDataParametersModule
        implements ExternalJobParametersModule {

    /* just for input; will be deleted after munging */
    public final static String DATA_FILE_PREFIX = 'DATA_FILE_PREFIX' /* not sure why it's called this */

    public final static String DATA_FILE = 'DATA_FILE' /* final destination for DATA_FILE_PREFIX */
    public final static String DATA_TYPE = 'DATA_TYPE'
    public final static String LOG_BASE = 'LOG_BASE'
    public final static String SRC_LOG_BASE = 'SRC_LOG_BASE'
    public final static String ALLOW_MISSING_ANNOTATIONS = 'ALLOW_MISSING_ANNOTATIONS'
    public final static String ZERO_MEANS_NO_INFO = 'ZERO_MEANS_NO_INFO'
    public static final Set<String> YES_NO = ['Y', 'N'] as Set
    public static final Set<String> DATA_TYPE_VALUES = ['R', 'L'] as Set

    Set<String> supportedParameters = ImmutableSet.of(
            DATA_FILE_PREFIX,
            DATA_FILE,
            DATA_TYPE,
            LOG_BASE,
            ALLOW_MISSING_ANNOTATIONS,
            ZERO_MEANS_NO_INFO,
            SRC_LOG_BASE,
    )

    void validate(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[LOG_BASE] == null || !ejp[LOG_BASE].isLong() || ejp[LOG_BASE] as Long != 2) {
            throw new InvalidParametersFileException("$LOG_BASE must be 2")
        }

        mandatory ejp, DATA_TYPE
        if (!DATA_TYPE_VALUES.contains(ejp[DATA_TYPE])) {
            throw new InvalidParametersFileException("${DATA_TYPE} equals ${ejp[DATA_TYPE]}," +
                    " but it has to be one of the following: ${DATA_TYPE_VALUES.join(', ')}")
        }

        if (ejp[DATA_TYPE] == 'L' && ejp[SRC_LOG_BASE] == null) {
            throw new InvalidParametersFileException("${SRC_LOG_BASE} has to be specified when ${DATA_TYPE}=L")
        }
        if (ejp[DATA_TYPE] != 'L' && ejp[SRC_LOG_BASE] != null) {
            throw new InvalidParametersFileException("${SRC_LOG_BASE} could be specified only when ${DATA_TYPE}=L")
        }

        if (ejp[DATA_FILE_PREFIX] && ejp[DATA_FILE]) {
            throw new InvalidParametersFileException(
                    "Can't set $DATA_FILE_PREFIX and $DATA_FILE simultaneously")
        }
        if (ejp[DATA_FILE_PREFIX] == null && ejp[DATA_FILE] == null) {
            throw new InvalidParametersFileException(
                    "Either $DATA_FILE_PREFIX or $DATA_FILE must be set")
        }
        if (!YES_NO.contains(ejp[ZERO_MEANS_NO_INFO])) {
            throw new InvalidParametersFileException("${ZERO_MEANS_NO_INFO} flag has invalid argument " +
                    "${ejp[ZERO_MEANS_NO_INFO]}. It has to be one of the following ${YES_NO.join(', ')}")
        }
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[LOG_BASE] == null) {
            ejp[LOG_BASE] = '2'
        }

        if (ejp[DATA_TYPE] == null) {
            ejp[DATA_TYPE] = 'R'
        }

        if (ejp[DATA_FILE_PREFIX]) {
            ejp[DATA_FILE] = ejp[DATA_FILE_PREFIX]
            ejp[DATA_FILE_PREFIX] = null
        }

        ejp[DATA_FILE] = convertRelativePath ejp, DATA_FILE

        mungeBoolean(ejp, ALLOW_MISSING_ANNOTATIONS, false)
        mungeBoolean(ejp, ZERO_MEANS_NO_INFO, false)
    }
}
