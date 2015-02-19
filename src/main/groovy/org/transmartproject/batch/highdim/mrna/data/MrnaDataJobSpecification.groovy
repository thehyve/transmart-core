package org.transmartproject.batch.highdim.mrna.data

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.*

/**
 * External parameters for main mrna data.
 */
@TypeChecked
class MrnaDataJobSpecification
        implements JobSpecification, ExternalJobParametersModule {

    /* just for input; will be deleted after munging */
    public final static String DATA_FILE_PREFIX = 'DATA_FILE_PREFIX' /* not sure why it's called this */

    public final static String DATA_FILE = 'DATA_FILE' /* final destination for DATA_FILE_PREFIX */
    public final static String DATA_TYPE = 'DATA_TYPE'
    public final static String LOG_BASE  = 'LOG_BASE'
    public final static String NODE_NAME = 'NODE_NAME'
    public final static String MAP_FILENAME = 'MAP_FILENAME'

    private static final String DEFAULT_NODE_NAME = 'MRNA'

    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new StudyJobParametersModule(),
            this]

    Set<String> supportedParameters = ImmutableSet.of(
            DATA_FILE_PREFIX,
            DATA_FILE,
            DATA_TYPE,
            LOG_BASE,
            NODE_NAME,
            MAP_FILENAME,)


    final Class jobPath = MrnaDataJobConfiguration

    void validate(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[LOG_BASE] == null) {
            ejp[LOG_BASE] = 2
        } else if (!ejp[LOG_BASE].isLong() || ejp[LOG_BASE] as Long != 2) {
            throw new InvalidParametersFileException("$LOG_BASE must be 2")
        }

        ejp.mandatory DATA_TYPE
        if (ejp[DATA_TYPE] != 'R') {
            throw new InvalidParametersFileException("$DATA_TYPE must be 'R'")
        }

        if (ejp[DATA_FILE_PREFIX] && ejp[DATA_FILE]) {
            throw new InvalidParametersFileException(
                    "Cannot set $DATA_FILE_PREFIX and $DATA_FILE simultaneously")
        }
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp[NODE_NAME] = ejp[NODE_NAME] ?: DEFAULT_NODE_NAME

        if (ejp[DATA_FILE_PREFIX]) {
            ejp[DATA_FILE] = ejp[DATA_FILE_PREFIX]
            ejp[DATA_FILE_PREFIX] = null
        }

        [DATA_FILE, MAP_FILENAME].each { p ->
            if (ejp[p] == 'x') {
                ejp[p] == null
            } else {
                ejp[p] = ejp.convertRelativePath p
            }
        }
    }
}
