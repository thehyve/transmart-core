/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.artefact.Controller
import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.rest.misc.CurrentUser

abstract class AbstractQueryController implements Controller {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    @Autowired
    MultidimensionalDataService multidimensionalDataService
    
    def conceptsResourceService

    static def globalParams = [
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
    static void checkParams(Map parameters, Collection<String> acceptedParameters) {
        def acceptedParams = (globalParams as Set) + acceptedParameters
        def unacceptableParams = parameters.keySet() - acceptedParams
        if (!unacceptableParams.empty) {
            if (unacceptableParams.size() == 1) {
                throw new InvalidArgumentsException("Parameter not supported: ${unacceptableParams.first()}.")
            } else {
                throw new InvalidArgumentsException("Parameters not supported: ${unacceptableParams.join(', ')}.")
            }
        }
    }

    protected static Constraint getConstraintFromStringOrJson(constraintParam) {
        if (!constraintParam) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }

        if (constraintParam instanceof String) {
            try {
                def constraintData = JSON.parse(constraintParam) as Map
                return ConstraintFactory.create(constraintData)
            } catch (ConverterException c) {
                throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintParam")
            }
        } else {
            return ConstraintFactory.create(constraintParam)
        }
    }

    protected Constraint bindConstraint(constraintParam) {
        Constraint constraint = getConstraintFromStringOrJson(constraintParam)

        // check for parse errors
        if (constraint.hasErrors()) {
            response.status = 400
            if(constraint.errors.fieldErrors.any()){
                throw new InvalidArgumentsException(constraint.errors.fieldErrors.first().defaultMessage)
            }
            render constraint.errors as JSON
            return null
        }
        // check for validation errors
        constraint.validate()
        if (constraint.hasErrors()) {
            response.status = 400
            render constraint.errors as JSON
            return null
        }
        return constraint
    }

    /**
     * Gets arguments from received REST request
     * either from queryString for GET method
     * or from request body for POST method.
     *
     * @return Map with passed arguments
     */
    protected Map getGetOrPostParams() {
        if(request.method == "POST") {
            return request.JSON as Map
        }
        return params.collectEntries { String k, v ->
            if (!globalParams.contains(k))
                [k, URLDecoder.decode(v, 'UTF-8')]
            else [:]
        }
    }
}
