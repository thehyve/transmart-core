package org.transmartproject.batch.startup

import org.springframework.context.i18n.LocaleContextHolder

/**
 * Parameters common to study based jobs (i.e. not annotations).
 */
final class StudyJobParametersModule implements ExternalJobParametersModule {
    public static final String STUDY_ID = 'STUDY_ID' /* can be a platform name */
    public static final String TOP_NODE = 'TOP_NODE'
    public static final String SECURITY_REQUIRED = 'SECURITY_REQUIRED'

    final Set<String> supportedParameters =
            [STUDY_ID, TOP_NODE, SECURITY_REQUIRED]

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (!ejp[STUDY_ID]) {
            def absolutePath = ejp.filePath.toAbsolutePath()
            def count = absolutePath.nameCount
            if (count < 2) {
                throw new InvalidParametersFileException("Could not " +
                        "determine study id from path ${absolutePath}")
            }
            ejp[STUDY_ID] = absolutePath.subpath(count - 2, count - 1)
        }

        ejp.mungeBoolean(SECURITY_REQUIRED, false)

        if (!ejp[TOP_NODE]) {
            def prefix = ejp[SECURITY_REQUIRED] == 'Y' ?
                    'Private Studies' :
                    'Public Studies'

            ejp[TOP_NODE] = "\\$prefix\\${ejp[STUDY_ID]}\\"
        }

        // should come last so the proper case is preserved for TOP_NODE
        ejp[STUDY_ID] = ejp[STUDY_ID].toUpperCase(LocaleContextHolder.locale)
    }
}
