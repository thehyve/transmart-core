/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.metadata.LegacyStudyException
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.LazyOutputStreamDecorator

import static org.transmartproject.rest.MultidimensionalDataSerialisationService.*

@Slf4j
class QueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    HighDimensionResourceService highDimensionResourceService

    protected Format getContentFormat() {
        Format format = Format.NONE
        withFormat {
            json {
                format = Format.JSON
            }
            protobuf {
                format = Format.PROTOBUF
            }
        }
        format
    }

    /**
     * Hypercube endpoint:
     *
     * For clinical data:
     * <code>/v2/observations?type=clinical&constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     *
     * For high dimensional data:
     * <code>/v2/high_dim?type=${type}&constraint=${assays}&biomarker_constraint=${biomarker}&projection=${projection}</code>
     *
     * The type must be the data type name of a high dimension type, or 'autodetect'. The latter will automatically
     * try to detect the datatype based on the assay constraint. If there are multiple possible types an error is
     * returned.
     *
     * Expects a {@link Constraint} parameter <code>constraint</code> and a supported
     * projection name (see {@link org.transmartproject.core.dataquery.highdim.projections.Projection}.
     *
     * The optional {@link Constraint} parameter <code>biomarker_constraint</code> allows filtering on biomarkers, e.g.,
     * chromosomal regions and gene names.
     *
     * @return a hypercube representing the observations that satisfy the constraint.
     */
    def observations() {
        checkParams(params, ['type', 'constraint', 'assay_constraint', 'biomarker_constraint', 'projection', 'pack'])

        if (params.type == null) throw new InvalidArgumentsException("Parameter 'type' is required")

        boolean pack = params.pack =! 'false'

        if (params.type == 'clinical') {
            clinicalObservations(params.constraint, pack)
        } else {
            if(params.assay_constraint) {
                response.sendError(422, "Parameter 'assay_constraint' is no longer used, use 'constraint' instead")
                return
            }
            highdimObservations(params.type, params.constraint, params.biomarker_constraint, params.projection, pack)
        }
    }

    /**
     * Helper function for retrieving clinical hypercube data
     */
    private def clinicalObservations(constraint_text, boolean pack) {

        def format = contentFormat
        if (format == Format.NONE) {
            throw new InvalidArgumentsException("Format not supported.")
        }
        Constraint constraint = bindConstraint(constraint_text)
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
            multidimensionalDataSerialisationService.serialise(result, format, out, pack)
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
        checkParams(params, ['constraint'])

        Constraint constraint = bindConstraint(params.constraint)
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
        checkParams(params, ['constraint', 'type'])

        if (!params.type) {
            throw new InvalidArgumentsException("Type parameter is missing.")
        }
        Constraint constraint = bindConstraint(params.constraint)
        if (constraint == null) {
            return
        }
        def aggregateType = AggregateType.forName(params.type as String)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = queryService.aggregate(aggregateType, constraint, user)
        def result = [(aggregateType.name().toLowerCase()): aggregatedValue]
        render result as JSON
    }

    /**
     * Helper function for retrieving high dimensional hypercube data.
     *
     * Expects a {@link Constraint} parameter <code>assay_constraint</code> and a supported
     * projection name (see {@link org.transmartproject.core.dataquery.highdim.projections.Projection}.
     *
     * The optional {@link Constraint} parameter <code>biomarker_constraint</code> allows filtering on biomarkers, e.g.,
     * chromosomal regions and gene names.
     *
     * @return a hypercube representing the high dimensional data that satisfies the constraints.
     */
    private def highdimObservations(String type, String assay_constraint, String biomarker_constraint, projection,
                                    boolean pack) {

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        Constraint assayConstraint = parseConstraint(URLDecoder.decode(assay_constraint, 'UTF-8'))

        BiomarkerConstraint biomarkerConstraint = biomarker_constraint ?
                (BiomarkerConstraint) parseConstraint(URLDecoder.decode(biomarker_constraint, 'UTF-8')) : new BiomarkerConstraint()

        Hypercube hypercube = queryService.highDimension(assayConstraint, biomarkerConstraint, projection, user, type)

        def format = contentFormat
        OutputStream out = new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = format.toString()
                    response.outputStream
                })
        try {
            multidimensionalDataSerialisationService.serialise(hypercube, format, out, pack)
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
        checkParams(params, [])

        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }

}
