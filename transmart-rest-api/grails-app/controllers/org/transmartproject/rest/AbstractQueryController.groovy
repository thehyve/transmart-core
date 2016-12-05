package org.transmartproject.rest

import grails.artefact.Controller
import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.Constraint
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
    MultidimensionalDataSerialisationService multidimensionalDataSerialisationService

    def conceptsResourceService

    protected static Constraint parseConstraint(String constraintText) {
        try {
            Map constraintData = JSON.parse(constraintText) as Map
            try {
                return ConstraintFactory.create(constraintData)
            } catch (Exception e) {
                throw new InvalidArgumentsException(e.message)
            }
        } catch (ConverterException e) {
            throw new InvalidArgumentsException('Cannot parse constraint parameter.')
        }
    }

    protected Constraint getConstraint(String constraintParameterName = 'constraint') {
        if (!params.containsKey(constraintParameterName)) {
            throw new InvalidArgumentsException("${constraintParameterName} parameter is missing.")
        }
        if (!params[constraintParameterName]) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }
        String constraintParam = URLDecoder.decode(params[constraintParameterName], 'UTF-8')
        parseConstraint(constraintParam)
    }

    protected Constraint bindConstraint() {
        Constraint constraint = getConstraint()
        // check for parse errors
        if (constraint.hasErrors()) {
            response.status = 400
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
