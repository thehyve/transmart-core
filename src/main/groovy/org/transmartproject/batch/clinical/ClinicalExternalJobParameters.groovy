package org.transmartproject.batch.clinical

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Processes the clinical.params files.
 */
@TypeChecked
final class ClinicalExternalJobParameters extends ExternalJobParameters {
    public final static String COLUMN_MAP_FILE = 'COLUMN_MAP_FILE'
    public final static String WORD_MAP_FILE = 'WORD_MAP_FILE'
    public final static String RECORD_EXCLUSION_FILE = 'RECORD_EXCLUSION_FILE'
    public final static String XTRIAL_FILE = 'XTRIAL_FILE'
    public final static String TAGS_FILE = 'TAGS_FILE'

    final Class jobPath = ClinicalDataLoadJobConfiguration

    final Set<String> fileParameterKeys = ImmutableSet.of(
            COLUMN_MAP_FILE,
            WORD_MAP_FILE,
            RECORD_EXCLUSION_FILE,
            XTRIAL_FILE,
            TAGS_FILE
    )

    @Override
    void validate() throws InvalidParametersFileException {
        mandatory COLUMN_MAP_FILE
    }

    @Override
    void doMunge() throws InvalidParametersFileException {
        this[COLUMN_MAP_FILE] = convertRelativePath COLUMN_MAP_FILE

        [WORD_MAP_FILE,
         RECORD_EXCLUSION_FILE,
         XTRIAL_FILE,
         TAGS_FILE].each { p ->
            if (this[p] == 'x') {
                this[p] == null
            } else {
                this[p] = convertRelativePath p
            }
        }
    }
}
