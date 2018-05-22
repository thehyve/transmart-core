package org.transmartproject.rest.misc

import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Contains http request utilities.
 */
class RequestUtils {

    final static Set<String> GLOBAL_PARAMS = [
            "controller",
            "action",
            "format",
            "apiVersion"
    ]

    /**
     * Checks if there are any parameters that are not in the set of default parameters
     * (format, action, controller, apiVersion) or the set of additional parameters for
     * the endpoint passed in <code>acceptedParameters</code>.
     *
     * @param parameters the request parameters
     * @param acceptedParameters the collection of supported non-default parameters.
     * @throws InvalidArgumentsException iff a parameter is used that is not supported.
     */
    static void checkForUnsupportedParams(Map parameters, Collection<String> acceptedParameters) {
        def acceptedParams = GLOBAL_PARAMS + acceptedParameters
        def unacceptableParams = parameters.keySet() - acceptedParams
        if (!unacceptableParams.empty) {
            if (unacceptableParams.size() == 1) {
                throw new InvalidArgumentsException("Parameter not supported: ${unacceptableParams.first()}.")
            } else {
                throw new InvalidArgumentsException("Parameters not supported: ${unacceptableParams.join(', ')}.")
            }
        }
    }

    /**
     * Parse a string as JSON. If that fails, throw an InvalidArgumentsException
     *
     * @Deprecated Explicit parsing to arbitrary objects should be replaced by
     * proper deserialisation to objects of a specific type, e.g., using ObjectMapper.
     *
     * @param str The JSON string or null
     * @return A JSON datastructure of maps and lists, or null if the input was null
     * @throws InvalidArgumentsException if parsing as JSON failed
     */
    @Deprecated
    static def parseJson(String str) {
        if (str == null) return null
        try {
            return new JsonSlurper().parseText(str)
        } catch (JsonException | IllegalArgumentException e) {
            throw new InvalidArgumentsException("Invalid JSON: '$str'", e)
        }
    }

}
