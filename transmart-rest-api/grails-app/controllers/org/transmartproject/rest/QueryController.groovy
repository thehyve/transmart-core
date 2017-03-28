/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.multidimquery.AggregateType
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.LazyOutputStreamDecorator

import static MultidimensionalDataService.*

@Slf4j
class QueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    MultidimensionalDataService multidimensionalDataService

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
        def constraintMap = request.method == "POST" ? request.JSON as Map : params
        checkParams(constraintMap, ['type', 'constraint', 'assay_constraint', 'biomarker_constraint', 'projection'])

        if (constraintMap.type == null) throw new InvalidArgumentsException("Parameter 'type' is required")

        if (constraintMap.type == 'clinical') {
            clinicalObservations(constraintMap.constraint.toString())
        } else {
            if(constraintMap.assay_constraint) {
                response.sendError(422, "Parameter 'assay_constraint' is no longer used, use 'constraint' instead")
                return
            }
            highdimObservations(constraintMap.type as String,
                                constraintMap.constraint.toString(),
                                constraintMap.biomarker_constraint.toString(),
                                constraintMap.projection.toString())
        }
    }

    /**
     * Helper function for retrieving clinical hypercube data
     */
    private def clinicalObservations(constraint_text) {

        def format = contentFormat
        if (format == Format.NONE) {
            throw new InvalidArgumentsException("Format not supported.")
        }
        Constraint constraint = bindConstraint(constraint_text)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        OutputStream out = getLazyOutputStream(format)

        try {
            multidimensionalDataService.writeClinical(format, constraint, user, out)
        } catch(LegacyStudyException e) {
            throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
        } finally {
            out.close()
        }

        return false
    }

    private getLazyOutputStream(Format format) {
        new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = format.toString()
                    response.outputStream
                })
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
        def constraintMap = request.method == "POST" ? request.JSON as Map : params
        checkParams(constraintMap, ['constraint'])

        Constraint constraint = bindConstraint(constraintMap.constraint.toString())
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def count = multiDimService.count(constraint, user)
        def result = [count: count]
        render result as JSON
    }

    /**
     * Aggregate endpoint:
     * <code>/v2/observations/aggregate?type=${type}&constraint=${constraint}</code>
     *
     * Expects an {@link org.transmartproject.core.multidimquery.AggregateType} parameter <code>type</code> and {@link Constraint}
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
        def constraintMap = request.method == "POST" ? request.JSON as Map : params
        checkParams(constraintMap, ['constraint', 'type'])

        if (!constraintMap.type) {
            throw new InvalidArgumentsException("Type parameter is missing.")
        }
        Constraint constraint = bindConstraint(constraintMap.constraint.toString())
        if (constraint == null) {
            return
        }
        def aggregateType = AggregateType.forName(constraintMap.type as String)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = multiDimService.aggregate(aggregateType, constraint, user)
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
    private def highdimObservations(String type, String assay_constraint, String biomarker_constraint, String projection) {

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        MultiDimConstraint assayConstraint = parseConstraint(URLDecoder.decode(assay_constraint, 'UTF-8'))

        MultiDimConstraint biomarkerConstraint = biomarker_constraint ?
                (MultiDimConstraint) parseConstraint(URLDecoder.decode(biomarker_constraint, 'UTF-8')) : new BiomarkerConstraint()

        Format format = contentFormat
        OutputStream out = getLazyOutputStream(format)

        try {
            multidimensionalDataService.writeHighdim(format, type, assayConstraint, biomarkerConstraint, projection, user, out)
        } finally {
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
