package org.transmartproject.batch.clinical

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.startup.InvalidParametersFileException

@TypeChecked
final class ClinicalExternalJobParameters extends ExternalJobParameters {
    public final static String COLUMN_MAP_FILE = 'COLUMN_MAP_FILE'
    public final static String WORD_MAP_FILE = 'WORD_MAP_FILE'
    public final static String RECORD_EXCLUSION_FILE = 'RECORD_EXCLUSION_FILE'

    final Set<String> fileParameterKeys = ImmutableSet.of(
            COLUMN_MAP_FILE,
            WORD_MAP_FILE,
            RECORD_EXCLUSION_FILE,
    )

    @Override
    void validate() throws InvalidParametersFileException {
        mandatory COLUMN_MAP_FILE
    }

    @Override
    void doMunge() throws InvalidParametersFileException {
        this[COLUMN_MAP_FILE] = convertRelativePath COLUMN_MAP_FILE

        if (this[WORD_MAP_FILE] == 'x') {
            this[WORD_MAP_FILE] == null
        } else {
            this[WORD_MAP_FILE] = convertRelativePath WORD_MAP_FILE
        }

        if (this[RECORD_EXCLUSION_FILE] == 'x') {
            this[RECORD_EXCLUSION_FILE] == null
        } else {
            this[RECORD_EXCLUSION_FILE] = convertRelativePath RECORD_EXCLUSION_FILE
        }
    }

    @Override
    Class getJobPath() {
        ClinicalDataLoadJobConfiguration
    }
}
