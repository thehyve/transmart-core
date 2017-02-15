/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.artefact.Controller
import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintBindingException
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.rest.misc.CurrentUser

abstract class AbstractQueryController implements Controller {

    @Autowired
    QueryService queryService

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

    protected static Constraint parseConstraint(String constraintText) {
        try {
            Map constraintData = JSON.parse(constraintText) as Map
            try {
                return ConstraintFactory.create(constraintData)
            } catch (ConstraintBindingException e) {
                throw e
            } catch (Exception e) {
                throw new InvalidArgumentsException(e.message)
            }
        } catch (ConverterException e) {
            throw new InvalidArgumentsException('Cannot parse constraint parameter.')
        }
    }

    protected Constraint getConstraint(String constraint, String paramName = 'constraint') {
        if (constraint == null) {
            throw new InvalidArgumentsException("${paramName} parameter is missing.")
        }
        if (!constraint) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }
        String constraintParam = URLDecoder.decode(constraint, 'UTF-8')
        parseConstraint(constraintParam)
    }

    protected Constraint bindConstraint(String constraint_text) {
        Constraint constraint = getConstraint(constraint_text)
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


}
