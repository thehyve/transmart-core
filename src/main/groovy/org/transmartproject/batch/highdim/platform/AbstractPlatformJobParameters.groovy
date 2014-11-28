package org.transmartproject.batch.highdim.platform

import org.springframework.context.i18n.LocaleContextHolder
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Platform for loading an annotation.
 */
abstract class AbstractPlatformJobParameters extends ExternalJobParameters {
    public final static String PLATFORM = 'PLATFORM'
    public final static String TITLE = 'TITLE'
    public final static String ORGANISM = 'ORGANISM'
    public final static String ANNOTATIONS_FILE = 'ANNOTATIONS_FILE'
    public final static String MARKER_TYPE = 'MARKER_TYPE'

    private final static String DEFAULT_ORGANISM = 'Homo Sapiens'

    final Set<String> getFileParameterKeys() {
        [ANNOTATIONS_FILE] as Set
    }

    @Override
    void validate() throws InvalidParametersFileException {
        mandatory PLATFORM
        mandatory TITLE
        mandatory ANNOTATIONS_FILE
        if (this[MARKER_TYPE]) {
            throw new InvalidParametersFileException(
                    "Parameter $MARKER_TYPE is not user settable")
        }
    }

    abstract String getMarkerType()

    @Override
    protected void doMunge() throws InvalidParametersFileException {
        this[ANNOTATIONS_FILE] = convertRelativePath ANNOTATIONS_FILE

        this[ORGANISM] = this[ORGANISM] ?: DEFAULT_ORGANISM

        this[MARKER_TYPE] = markerType

        this[PLATFORM] = this[PLATFORM]?.toUpperCase(LocaleContextHolder.locale)
    }
}
