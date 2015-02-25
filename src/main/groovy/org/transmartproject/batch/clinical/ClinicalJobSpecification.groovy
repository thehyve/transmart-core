package org.transmartproject.batch.clinical

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException
import org.transmartproject.batch.startup.JobSpecification
import org.transmartproject.batch.startup.StudyJobParametersModule

/**
 * Parameter and configuration class specification for clinical jobs.
 */
final class ClinicalJobSpecification implements
        JobSpecification, ExternalJobParametersModule {

    public final static String COLUMN_MAP_FILE = 'COLUMN_MAP_FILE'
    public final static String WORD_MAP_FILE = 'WORD_MAP_FILE'
    public final static String RECORD_EXCLUSION_FILE = 'RECORD_EXCLUSION_FILE'
    public final static String XTRIAL_FILE = 'XTRIAL_FILE'
    public final static String TAGS_FILE = 'TAGS_FILE'

    List<? extends ExternalJobParametersModule> jobParametersModules = [
            new StudyJobParametersModule(),
            this,
    ]

    final Class<?> jobPath = ClinicalDataLoadJobConfiguration

    final Set<String> supportedParameters = ImmutableSet.of(
            COLUMN_MAP_FILE,
            WORD_MAP_FILE,
            RECORD_EXCLUSION_FILE,
            XTRIAL_FILE,
            TAGS_FILE)

    void validate(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp.mandatory COLUMN_MAP_FILE
    }

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        ejp[COLUMN_MAP_FILE] = ejp.convertRelativePath COLUMN_MAP_FILE

        [WORD_MAP_FILE,
         RECORD_EXCLUSION_FILE,
         XTRIAL_FILE,
         TAGS_FILE].each { p ->
            if (ejp[p] == 'x') {
                ejp[p] = null
            } else {
                ejp[p] = ejp.convertRelativePath p
            }
        }
    }
}
