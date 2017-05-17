/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.artefact.Controller
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintBindingException
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
        try {
            return getConstraintFromStringOrJson(constraintParam)
        } catch (ConstraintBindingException e) {
            if(e.errors?.fieldErrors?.any()) {

                // This representation is compatible with what is returned when an exception is not caught.
                //
                // I want to add properties to the `e.errors as JSON`, but getting at a map representation of e.errors
                // is not so easy. This is an ugly workaround, but this only happens for error conditions.
                Map error = new JsonSlurper().parseText((e.errors as JSON).toString())
                error = [
                        httpStatus: 400,
                        message: e.message,
                        type: e.class.simpleName,
                ] + error

                response.status = 400
                render error as JSON
                return null
            }
            throw e
        }
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
            if (!globalParams.contains(k)) {
                if(v instanceof Object[] || v instanceof List) {
                    [k, v.collect { URLDecoder.decode(it, 'UTF-8') }]
                } else {
                    [k, URLDecoder.decode(v, 'UTF-8')]
                }
            } else [:]
        }
    }
}
