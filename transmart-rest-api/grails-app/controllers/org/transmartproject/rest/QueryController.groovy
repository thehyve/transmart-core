package org.transmartproject.rest

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.multidimquery.HypercubeImpl
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.db.multidimquery.query.DimensionMetadata
import org.transmartproject.db.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.AggregateType
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser
import org.transmartproject.rest.misc.LazyOutputStreamDecorator

@Slf4j
class QueryController {

    @Autowired
    QueryService queryService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    @Autowired
    MultidimensionalDataSerialisationService multidimensionalDataSerialisationService

    @Autowired
    MultidimensionalDataResourceService queryResource

    def conceptsResourceService

    private Constraint getConstraint() {
        if (!params.constraint) {
            throw new InvalidArgumentsException('Constraint parameter is missing.')
        }
        String constraintParam = URLDecoder.decode(params.constraint, 'UTF-8')
        try {
            Map constraintData = JSON.parse(constraintParam) as Map
            try {
                return ConstraintFactory.create(constraintData)
            } catch(Exception e) {
                throw new InvalidArgumentsException(e.message)
            }
        } catch (ConverterException e) {
            throw new InvalidArgumentsException('Cannot parse constraint parameter.')
        }
    }

    private Constraint bindConstraint() {
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

    /**
     * Observations endpoint:
     * <code>/query/observations?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.ObservationFact} objects that
     * satisfy the constraint.
     */
    def observations() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def observations = queryService.list(constraint, user)
        render observations as JSON
    }

    /**
     * Hypercube endpoint:
     * <code>/query/hypercube?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a hypercube representing the observations that satisfy the constraint.
     */
    def hypercube() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        def dataType = 'clinical'
        HypercubeImpl result = queryResource.retrieveData(dataType, constraint: constraint)
        OutputStream out = new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = 'application/json'
                    response.outputStream
                })
        try {
            multidimensionalDataSerialisationService.writeData(result, "json", out)
        } finally {
            out.close()
        }
    }

    /**
     * Patients endpoint:
     * <code>/query/patients?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.PatientDimension} objects for
     * which there are observations that satisfy the constraint.
     */
    def patients() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def patients = queryService.listPatients(constraint, user)
        render patients as JSON
    }

    /**
     * Count endpoint:
     * <code>/query/count?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a the number of observations that satisfy the constraint.
     */
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

    /**
     * Aggregate endpoint:
     * <code>/query/aggregate?type=${type}&constraint=${constraint}</code>
     *
     * Expects an {@link AggregateType} parameter <code>type</code> and {@link Constraint}
     * parameter <code>constraint</code>.
     *
     * Checks if the supplied constraint contains a concept constraint on top level, because
     * aggregations is only valid for a single concept. If the concept is not found or
     * no observations are found for the concept, an {@link org.transmartproject.db.multidimquery.query.InvalidQueryException}
     * is thrown.
     * Also, if the concept is not numerical, has null values or values with an operator
     * other than 'E'.
     *
     * @return a map with the aggregate type as key and the result as value.
     */
    def aggregate() {
        if (!params.type) {
            throw new InvalidArgumentsException("Type parameter is missing.")
        }
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        def aggregateType = AggregateType.forName(params.type)
        User user = (User)usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = queryService.aggregate(aggregateType, constraint, user)
        def result = [(aggregateType.name().toLowerCase()): aggregatedValue]
        render result as JSON
    }

    /**
     * Supported fields endpoint:
     * <code>/query/supportedFields</code>
     *
     * @return the list of fields supported by {@link org.transmartproject.db.multidimquery.query.FieldConstraint}.
     */
    def supportedFields() {
        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }
}
