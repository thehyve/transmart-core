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
import org.transmartproject.rest.misc.RequestUtils

abstract class AbstractQueryController implements Controller {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    @Autowired
    HypercubeDataSerializationService hypercubeDataSerializationService

    protected static Constraint getConstraintFromString(String constraintText) {
        if (!constraintText) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }

        try {
            def constraint =  ConstraintFactory.read(constraintText)
            return constraint?.normalise()
        } catch (ConverterException c) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintText")
        }
    }

    protected Constraint bindConstraint(String constraintText) {
        try {
            return getConstraintFromString(constraintText)
        } catch (ConstraintBindingException e) {
            Map error = [
                    httpStatus: 400,
                    message   : e.message,
                    type      : e.class.simpleName,
            ]

            if (e.errors) {
                error.errors = e.errors
                        .collect { [propertyPath: it.propertyPath.toString(), message: it.message] }

            }

            response.status = 400
            render error as JSON
            return null
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
            def parameters = request.JSON as Map
            return parameters.collectEntries { String k, v ->
                if(v instanceof Object[] || v instanceof List) {
                    [k, v.collect { (it as JSON).toString(false) }]
                } else {
                    [k, (v as JSON).toString(false)]
                }
            }
        }
        return params.collectEntries { String k, v ->
            if (!RequestUtils.GLOBAL_PARAMS.contains(k)) {
                if(v instanceof Object[] || v instanceof List) {
                    [k, v.collect { URLDecoder.decode(it, 'UTF-8') }]
                } else {
                    [k, URLDecoder.decode(v, 'UTF-8')]
                }
            } else [:]
        }
    }
}
