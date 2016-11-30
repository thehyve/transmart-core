package org.transmartproject.rest

import grails.converters.JSON
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.UsersResource
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.metadata.LegacyStudyException
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser
import org.transmartproject.rest.misc.LazyOutputStreamDecorator
import org.transmartproject.rest.protobuf.ObservationsSerializer

@Slf4j
class QueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    HighDimensionResourceService highDimensionResourceService

    protected ObservationsSerializer.Format getContentFormat() {
        ObservationsSerializer.Format format = ObservationsSerializer.Format.NONE
        withFormat {
            json {
                format = ObservationsSerializer.Format.JSON
            }
            protobuf {
                format = ObservationsSerializer.Format.PROTOBUF
            }
        }
        format
    }

    /**
     * Observations endpoint:
     * <code>/v2/observation_list?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.ObservationFact} objects that
     * satisfy the constraint.
     */
    def observationList() {
        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def observations = queryService.list(constraint, user)
        render observations as JSON
    }

    /**
     * Hypercube endpoint:
     * <code>/v2/observations?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a hypercube representing the observations that satisfy the constraint.
     */
    def observations() {
        def format = contentFormat
        if (format == ObservationsSerializer.Format.NONE) {
            throw new InvalidArgumentsException("Format not supported.")
        }

        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Hypercube result
        try {
            result = queryService.retrieveClinicalData(constraint, user)
        } catch(LegacyStudyException e) {
            throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
        }

        log.info "Writing to format: ${format}"
        OutputStream out = new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = format.toString()
                    response.outputStream
                })
        try {
            multidimensionalDataSerialisationService.serialise(result, format, out)
        } finally {
            out.close()
        }
        return false
    }

    /**
     * Count endpoint:
     * <code>/v2/observations/count?constraint=${constraint}</code>
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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def count = queryService.count(constraint, user)
        def result = [count: count]
        render result as JSON
    }

    /**
     * Aggregate endpoint:
     * <code>/v2/observations/aggregate?type=${type}&constraint=${constraint}</code>
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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = queryService.aggregate(aggregateType, constraint, user)
        def result = [(aggregateType.name().toLowerCase()): aggregatedValue]
        render result as JSON
    }

    /**
     * High dimensional endpoint:
     * <code>/v2/high_dim?assay_constraint=${assays}&biomarker_constraint=${biomarker}&projection=${projection}</code>
     *
     * Expects a {@link Constraint} parameter <code>assay_constraint</code> and a supported
     * projection name (see {@link org.transmartproject.core.dataquery.highdim.projections.Projection}.
     *
     * The optional {@link Constraint} parameter <code>biomarker_constraint</code> allows filtering on biomarkers, e.g.,
     * chromosomal regions and gene names.
     *
     * @return a hypercube representing the high dimensional data that satisfies the constraints.
     */
    def highDim() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Constraint assayConstraint = getConstraint('assay_constraint')

        BiomarkerConstraint biomarkerConstraint = null
        if (params.biomarker_constraint) {
            Constraint constraint = getConstraint('biomarker_constraint')
            assert constraint instanceof BiomarkerConstraint
            biomarkerConstraint = constraint
        }

        Hypercube hypercube = queryService.highDimension(user, assayConstraint,
                biomarkerConstraint,
                params.projection)

        def format = contentFormat
        OutputStream out = new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = format.toString()
                    response.outputStream
                })
        try {
            multidimensionalDataSerialisationService.serialise(hypercube, format, out)
        } finally {
            hypercube.close()
            out.close()
        }
    }


    /**
     * Supported fields endpoint:
     * <code>/v2/supportedFields</code>
     *
     * @return the list of fields supported by {@link org.transmartproject.db.multidimquery.query.FieldConstraint}.
     */
    def supportedFields() {
        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }

}
