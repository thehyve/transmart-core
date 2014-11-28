package org.transmartproject.batch.tag

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.startup.ExternalJobParameters
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Processes the tags.params files.
 */
@TypeChecked
final class TagsLoadJobParameters extends ExternalJobParameters {
    public final static String TAGS_FILE = 'TAGS_FILE'

    final Class jobPath = TagsLoadJobConfiguration

    final Set<String> fileParameterKeys = ImmutableSet.of(TAGS_FILE)

    @Override
    void validate() throws InvalidParametersFileException {}

    @Override
    void doMunge() throws InvalidParametersFileException {
        if (this[TAGS_FILE] == 'x') {
            this[TAGS_FILE] == null
        }
        if (!this[TAGS_FILE]) {
            def files = filePath
                    .toAbsolutePath()
                    .parent
                    .resolve(typeName)
                    .toFile().listFiles()
            if (files.length == 1) {
                new InvalidParametersFileException("""Single file is expected, but found: ${files}.
                    NOTE: You could specify right file with ${TAGS_FILE} property in tags.param
                """)
            }
            this[TAGS_FILE] = files[0].toPath()
        } else {
            this[TAGS_FILE] = convertRelativePath TAGS_FILE
        }
    }
}
