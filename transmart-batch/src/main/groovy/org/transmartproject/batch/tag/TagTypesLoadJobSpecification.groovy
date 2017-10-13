package org.transmartproject.batch.tag

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.*

/**
 * Processes the tagtypes.params files.
 */
@TypeChecked
final class TagTypesLoadJobSpecification
        implements ExternalJobParametersModule, JobSpecification {

    public final static String TAG_TYPES_FILE = 'TAG_TYPES_FILE'

    final List<? extends ExternalJobParametersModule> jobParametersModules = [this]

    final Class jobPath = TagTypesLoadJobConfiguration

    final Set<String> supportedParameters = ImmutableSet.of(TAG_TYPES_FILE)

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[TAG_TYPES_FILE] == 'x') {
            ejp[TAG_TYPES_FILE] == null
        }
        if (!ejp[TAG_TYPES_FILE]) {
            def files = ejp.filePath
                    .toAbsolutePath()
                    .parent
                    .resolve(ejp.typeName)
                    .toFile().listFiles()
            if (files.length != 1) {
                new InvalidParametersFileException("Single file is expected, " +
                        "but found: ${files} files. NOTE: You could specify the " +
                        "right file with ${TAG_TYPES_FILE} property in tagtypes.param")
            }
            ejp[TAG_TYPES_FILE] = files[0].toPath()
        } else {
            ejp[TAG_TYPES_FILE] = convertRelativePath ejp, TAG_TYPES_FILE
        }
    }
}
