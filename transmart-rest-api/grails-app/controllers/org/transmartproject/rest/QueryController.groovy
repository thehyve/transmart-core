package org.transmartproject.rest

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.dataquery2.QueryService
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.ConstraintFactory
import org.transmartproject.db.dataquery2.query.DimensionMetadata
import org.transmartproject.db.dataquery2.query.Field
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

@Slf4j
class QueryController {

    QueryService queryService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    private Constraint bindConstraint() {
        if (!params.constraint) {
            throw new InvalidArgumentsException("Constraint parameter is missing.")
        }
        Constraint constraint
        String constraintParam = URLDecoder.decode(params.constraint, 'UTF-8')
        try {
            Map constraintData = JSON.parse(constraintParam) as Map
            constraint = ConstraintFactory.create(constraintData)
            if (constraint == null) {
                throw new InvalidArgumentsException("Empty constraint parameter.")
            }
            constraint.validate()
            if (constraint.hasErrors()) {
                response.status = 400
                render constraint.errors as JSON
                return null
            }
        } catch (ConverterException e) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter", e)
        }
        return constraint
    }

    def observations() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def observations = queryService.list(constraint, user)
        render observations as JSON
    }

    def count() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def count = queryService.count(constraint, user)
        def result = [count: count]
        render result as JSON
    }

    def aggregate() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        def aggregateType = QueryType.forName(params.type)
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = queryService.aggregate(aggregateType, constraint, user)
        def result = [(aggregateType): aggregatedValue]
        render result as JSON
    }

    def supportedFields() {
        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }
}
