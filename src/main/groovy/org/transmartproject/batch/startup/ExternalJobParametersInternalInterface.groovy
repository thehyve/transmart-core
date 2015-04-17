package org.transmartproject.batch.startup

import java.nio.file.Path

/**
 * Base class for the job parameters traits. Contains the facilities that
 * the external job parameters traits can use to do their work as well
 * as the methods that should be implemented.
 */
interface ExternalJobParametersInternalInterface {

    /**
     * Get parameter value.
     */
    String getAt(String index)

    /**
     * Set a parameter value.
     */
    void putAt(String index, Object value)

    /**
     * Path of the params file.
     */
    Path getFilePath()

    /**
     * The name of the type ("clinical", "mrna" and so on)
     */
    String getTypeName()

    /**
     * Reads a parameter with a relative path value and turns it into an
     * absolute path.
     */
    Path convertRelativePath(String parameter)

    /**
     * Throws if the parameter is not present.
     */
    void mandatory(String parameter) throws InvalidParametersFileException

    /**
     * Munges a boolean parameter, which will be stored not as a boolean
     * parameter but as Y/N. For historical reasons, of course.
     *
     * Any value different from 0, false and N will be interpreted as Y.
     * @param parameter
     * @param defaultValue if not provided, whether to use Y (true) or F (false)
     */
    void mungeBoolean(String parameter, boolean defaultValue)
}
