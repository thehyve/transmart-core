package org.transmartproject.batch.i2b2

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.i2b2.misc.DateConverter
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

import java.text.SimpleDateFormat

/**
 * Specifies the parameters for the i2b2 job.
 */
class I2b2ParametersModule implements ExternalJobParametersModule {

    public static final String COLUMN_MAP_FILE    = 'COLUMN_MAP_FILE'
    public static final String WORD_MAP_FILE      = 'WORD_MAP_FILE'
    public static final String PATIENT_IDE_SOURCE = 'PATIENT_IDE_SOURCE'
    public static final String VISIT_IDE_SOURCE   = 'VISIT_IDE_SOURCE'
    public static final String PROVIDER_PATH      = 'PROVIDER_PATH'
    public static final String SOURCE_SYSTEM      = 'SOURCE_SYSTEM'
    public static final String DATE_FORMAT        = 'DATE_FORMAT'
    public static final String DOWNLOAD_DATE      = 'DOWNLOAD_DATE'
    public static final String INCREMENTAL        = 'INCREMENTAL'
    public static final String CRC_SCHEMA         = 'CRC_SCHEMA'
    public static final String PROJECT_ID         = 'PROJECT_ID'

    public static final int MAX_SIZE_OF_SOURCE_SYSTEM = 50
    public static final int MAX_SIZE_OF_IDE_SOURCE = 50
    public static final int MAX_SIZE_OF_PROVIDER_PATH = 700
    public static final int MAX_SIZE_OF_PROJECT_ID = 50

    public static final String DUMMY_IDE_SOURCE = 'UNSPECIFIED'
    private static final String DEFAULT_CRC_SCHEMA = 'i2b2demodata'
    private static final String DEFAULT_PROJECT_ID = 'default'

    final Set<String> supportedParameters = ImmutableSet.of(
            COLUMN_MAP_FILE,
            WORD_MAP_FILE,
            PATIENT_IDE_SOURCE,
            VISIT_IDE_SOURCE,
            PROVIDER_PATH,
            SOURCE_SYSTEM,
            DATE_FORMAT,
            DOWNLOAD_DATE,
            INCREMENTAL,
            CRC_SCHEMA,
            PROJECT_ID,
    )

    @Override
    void validate(ExternalJobParametersInternalInterface ejp) {
        mandatory(ejp, SOURCE_SYSTEM)
        checkSize(ejp, SOURCE_SYSTEM, MAX_SIZE_OF_SOURCE_SYSTEM)
        checkSize(ejp, VISIT_IDE_SOURCE, MAX_SIZE_OF_IDE_SOURCE)
        checkSize(ejp, PATIENT_IDE_SOURCE, MAX_SIZE_OF_IDE_SOURCE)
        checkSize(ejp, PROVIDER_PATH, MAX_SIZE_OF_PROVIDER_PATH)
        checkSize(ejp, PROJECT_ID, MAX_SIZE_OF_PROJECT_ID)

        if (ejp[DATE_FORMAT] != null) {
            try {
                new SimpleDateFormat(ejp[DATE_FORMAT], Locale.ENGLISH)
            } catch (IllegalArgumentException iae) {
                throw new InvalidParametersFileException("Not a valid date " +
                        "format for SimpleDateFormat: ${ejp[DATE_FORMAT]}")
            }
        }

        if (ejp[DOWNLOAD_DATE] != null) {
            DateConverter dateConverter = new DateConverter().with {
                dateFormat = ejp[DATE_FORMAT]
                init()
                it
            }

            try {
                dateConverter.parse ejp[DOWNLOAD_DATE]
            } catch (IllegalArgumentException iae) {
                throw new InvalidParametersFileException("Not a valid date " +
                        "for the configured format for the parameter " +
                        "DOWNLOAD_DATE: ${ejp[DOWNLOAD_DATE]}")
            }
        }
    }

    @Override
    void munge(ExternalJobParametersInternalInterface ejp) {
        ejp[COLUMN_MAP_FILE] = convertRelativePath ejp, COLUMN_MAP_FILE

        if (ejp[WORD_MAP_FILE] == 'x' || ejp[WORD_MAP_FILE] == null) {
            ejp[WORD_MAP_FILE] = null
        } else {
            ejp[WORD_MAP_FILE] = convertRelativePath ejp, WORD_MAP_FILE
        }

        if (ejp[PATIENT_IDE_SOURCE] == null) {
            ejp[PATIENT_IDE_SOURCE] = DUMMY_IDE_SOURCE
        }

        if (ejp[VISIT_IDE_SOURCE] == null) {
            ejp[VISIT_IDE_SOURCE] = DUMMY_IDE_SOURCE
        }

        if (ejp[PROVIDER_PATH] == null) {
            ejp[PROVIDER_PATH] = '/'
        }

        if (ejp[CRC_SCHEMA] == null) {
            ejp[CRC_SCHEMA] = DEFAULT_CRC_SCHEMA
        }

        if (ejp[PROJECT_ID] == null) {
            ejp[PROJECT_ID] = DEFAULT_PROJECT_ID
        }

        mungeBoolean(ejp, INCREMENTAL, false)
    }

    private void checkSize(ExternalJobParametersInternalInterface ejp,
                           String parameter,
                           int maxSize) {
        if (ejp[parameter] != null && ejp[parameter].size() > maxSize) {
            throw new InvalidParametersFileException(
                    "Parameter $parameter cannot have more than " +
                            "$maxSize UTF-16 code units.")
        }
    }

}
