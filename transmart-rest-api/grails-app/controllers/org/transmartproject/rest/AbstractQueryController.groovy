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
        def acceptedParams = (['format', 'action', 'controller', 'apiVersion'] as Set) + acceptedParameters
        def unacceptableParams = parameters.keySet() - acceptedParams
        if (!unacceptableParams.empty) {
            if (unacceptableParams.size() == 1) {
                throw new InvalidArgumentsException("Parameter not supported: ${unacceptableParams.first()}.")
            } else {
                throw new InvalidArgumentsException("Parameters not supported: ${unacceptableParams.join(', ')}.")
            }
        }
    }

    protected static Constraint parseConstraintFromUrlStringOrJson(constraintParam) {
        try {
            if (constraintParam instanceof String) {
                def constraint_text = URLDecoder.decode(constraintParam, 'UTF-8')
                def constraintData = JSON.parse(constraint_text) as Map
                parseConstraint(constraintData)
            } else {
                parseConstraint(constraintParam)
            }
        }
        catch (ConverterException e) {
            throw new InvalidArgumentsException('Cannot parse constraint parameter.')
        }
    }

    protected static Constraint parseConstraint(constraintData) {
        try {
            return ConstraintFactory.create(constraintData)
        } catch (ConstraintBindingException e) {
            throw e
        } catch (Exception e) {
            throw new InvalidArgumentsException(e.message)
        }
    }

    protected static Constraint getConstraint(constraint, String paramName = 'constraint') {
        if (constraint == null) {
            throw new InvalidArgumentsException("${paramName} parameter is missing.")
        }
        if (!constraint) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }
        parseConstraintFromUrlStringOrJson(constraint)
    }

    protected Constraint bindConstraint(constraintParam) {
        Constraint constraint = getConstraint(constraintParam)

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
    protected Map getArgs() {
        request.method == "POST" ? request.JSON as Map : params
    }
}
