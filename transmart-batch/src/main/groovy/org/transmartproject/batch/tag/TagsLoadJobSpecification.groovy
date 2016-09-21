package org.transmartproject.batch.tag

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.*

/**
 * Processes the tags.params files.
 */
@TypeChecked
final class TagsLoadJobSpecification
        implements ExternalJobParametersModule, JobSpecification {

    public final static String TAGS_FILE = 'TAGS_FILE'

    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new StudyJobParametersModule(),
            this]

    final Class jobPath = TagsLoadJobConfiguration

    final Set<String> supportedParameters = ImmutableSet.of(TAGS_FILE)

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[TAGS_FILE] == 'x') {
            ejp[TAGS_FILE] == null
        }
        if (ejp[TAGS_FILE]) {
            ejp[TAGS_FILE] = convertRelativePath ejp, TAGS_FILE
        } else {
            def files = ejp.filePath
                    .toAbsolutePath()
                    .parent
                    .resolve(ejp.typeName)
                    .toFile().listFiles()
            if (files.length != 1) {
                new InvalidParametersFileException("Single file is expected, " +
                        "but found: ${files} files. NOTE: You could specify the " +
                        "right file with ${TAGS_FILE} property in tags.params")
            }
            ejp[TAGS_FILE] = files[0].toPath()
        }
    }
}
