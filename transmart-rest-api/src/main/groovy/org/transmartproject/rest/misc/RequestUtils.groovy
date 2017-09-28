package org.transmartproject.rest.misc

import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Contains http request utilities.
 */
class RequestUtils {

    final static GLOBAL_PARAMS = [
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
        def acceptedParams = (GLOBAL_PARAMS as Set) + acceptedParameters
        def unacceptableParams = parameters.keySet() - acceptedParams
        if (!unacceptableParams.empty) {
            if (unacceptableParams.size() == 1) {
                throw new InvalidArgumentsException("Parameter not supported: ${unacceptableParams.first()}.")
            } else {
                throw new InvalidArgumentsException("Parameters not supported: ${unacceptableParams.join(', ')}.")
            }
        }
    }

}
