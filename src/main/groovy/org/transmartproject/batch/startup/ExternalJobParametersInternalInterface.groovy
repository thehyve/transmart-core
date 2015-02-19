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
}
