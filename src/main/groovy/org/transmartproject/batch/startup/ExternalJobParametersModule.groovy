package org.transmartproject.batch.startup

import java.nio.file.Files
import java.nio.file.Path

/**
 * A class supporting a certain set of parameters.
 * A trait for having a default implementation before Java 8.
 */
@SuppressWarnings('BracesForClassRule')
// buggy with traits
@SuppressWarnings('UnusedMethodParameter')
@SuppressWarnings('EmptyMethod')
trait ExternalJobParametersModule {

    abstract Set<String> getSupportedParameters()

    /**
     * May be used by subclasses to change the parameter values into a canonical
     * form and add/remove parameters.
     */
    void munge(ExternalJobParametersInternalInterface ejp) {}

    void validate(ExternalJobParametersInternalInterface ejp) {}

    /**
     * Reads a parameter with a relative path value and turns it into an
     * absolute path.
     */
    static Path convertRelativePath(ExternalJobParametersInternalInterface ejp, String parameter) {
        def fileName = ejp[parameter]
        if (fileName == null) {
            return null
        }

        Path parent = ejp.filePath.toAbsolutePath().parent
        def file = parent.resolve(fileName)
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            file = parent.resolve(ejp.typeName).resolve(fileName)
        }

        if (!Files.isRegularFile(file) ||
                !Files.isReadable(file)) {
            throw new InvalidParametersFileException(
                    "Parameter ${parameter} references ${fileName}, but there is no such regular readable file.")
        }

        file
    }

    /**
     * Throws if the parameter is not present.
     */
    static void mandatory(ExternalJobParametersInternalInterface ejp, String parameter) {
        if (ejp[parameter] == null) {
            throw new InvalidParametersFileException(
                    "Parameter $parameter mandatory but not defined")
        }
    }

    /**
     * Munges a boolean parameter, which will be stored not as a boolean
     * parameter but as Y/N. For historical reasons, of course.
     *
     * @param ejp parameter/argument map
     * @param parameter parameter to munge
     * @param defaultValue if not provided, whether to use Y (true) or F (false)
     */
    static void mungeBoolean(ExternalJobParametersInternalInterface ejp, String parameter, boolean defaultValue) {
        if (ejp[parameter] == null) {
            ejp[parameter] = defaultValue ? 'Y' : 'N'
        } else if (ejp[parameter].toUpperCase() in ['N', 'NO']) {
            ejp[parameter] = 'N'
        } else if (ejp[parameter].toUpperCase() in ['Y', 'YES']) {
            ejp[parameter] = 'Y'
        } else {
            throw new InvalidParametersFileException(
                    "Unexpected argument ${ejp[parameter]} for boolean parameter ${parameter}.")
        }
    }
}
