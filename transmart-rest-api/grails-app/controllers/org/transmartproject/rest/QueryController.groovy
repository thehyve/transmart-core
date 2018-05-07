/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import com.fasterxml.jackson.core.type.TypeReference
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.dataquery.SortSpecification
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.exceptions.OperationNotImplementedException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CategoricalValueAggregates
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.NumericalValueAggregates
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.LazyOutputStreamDecorator
import org.transmartproject.rest.serialization.CrossTableSerializer
import org.transmartproject.rest.serialization.Format

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams
import static org.transmartproject.rest.misc.RequestUtils.parseJson

@Slf4j
class QueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    @Autowired
    AggregateDataResource aggregateDataResource

    @Autowired
    CrossTableResource crossTableResource


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
     * <code>/v2/observations?type=${type}&constraint=${assays}&biomarker_constraint=${biomarker}&projection=${projection}</code>
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
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['type', 'constraint', 'assay_constraint', 'biomarker_constraint',
                                         'projection', 'sort'])

        if (args.type == null) throw new InvalidArgumentsException("Parameter 'type' is required")

        if (args.type == 'clinical') {
            clinicalObservations(args.constraint, args.sort)
        } else {
            if(args.assay_constraint) {
                response.sendError(422, "Parameter 'assay_constraint' is no longer used, use 'constraint' instead")
                return
            }
            if(args.sort) {
                throw new UnsupportedByDataTypeException("Sorting is currently not supported for high dimensional data")
            }
            highdimObservations(args.type, args.constraint, args.biomarker_constraint, args.projection)
        }
    }

    static final TypeReference<List<SortSpecification>> sortListTypeReference =
            new TypeReference<List<SortSpecification>>(){}

    /**
     * Helper function for retrieving clinical hypercube data
     */
    private def clinicalObservations(String constraint_text, String sort_text) {

        def format = contentFormat
        if (format == Format.NONE) {
            throw new InvalidArgumentsException("Format not supported.")
        }
        Constraint constraint = bindConstraint(constraint_text)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        def sort = BindingHelper.readList(sort_text, sortListTypeReference)

        OutputStream out = getLazyOutputStream(format)

        try {
            def args = new DataRetrievalParameters(constraint: constraint, sort: sort)
            hypercubeDataSerializationService.writeClinical(format, args, user, out)
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
     * Data table endpoint:
     * <code>/v2/observations/table?type=${type}&constraint=${constraint}&rowDimensions=${rowDimensions}&
     * columnDimensions=${columnDimensions}&rowSort=${rowSort}&columnSort=${columnSort}&limit={limit}&offset={offset}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * The type should be the data type name of a high dimension type, or 'autodetect'.
     * Only 'clinical' type is currently supported.
     *
     * Expects columnDimensions and rowDimensions parameters.
     *
     * Optional rowSort and columnSort parameters allow to define the sorting.
     *
     * Pagination is supported via limit and offset parameters.
     *
     * @return a tabular representation of hypercube in a json format.
     */
    def table() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['type', 'constraint', 'rowDimensions', 'columnDimensions',
                                         'rowSort', 'columnSort', 'limit', 'offset'])

        if (args.type != 'clinical') { throw new OperationNotImplementedException("High dimensional data is not yet " +
                "implemented for the data table")
        }

        Constraint constraint = bindConstraint((String) args.constraint)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        if (args.limit == null) {
            throw new InvalidArgumentsException("Parameter 'limit' is required")
        }
        int limit = Integer.parseInt((String) args.limit)
        Long offset = args.offset ? Long.parseLong((String) args.offset) : 0

        def rowSort = BindingHelper.readList((String)args.rowSort, sortListTypeReference)
        def columnSort = BindingHelper.readList((String)args.columnSort, sortListTypeReference)
        def rowDimensions = parseIfJson(args.rowDimensions)
        def columnDimensions = parseIfJson(args.columnDimensions)

        [rowDimensions: rowDimensions, columnDimensions: columnDimensions].each { name, list ->
            if(! list instanceof List || list.any { ! it instanceof String }) {
                throw new InvalidArgumentsException("$name must be a JSON array of strings")
            }
        }

        OutputStream out = getLazyOutputStream(Format.JSON)

        TableConfig tableConfig = new TableConfig(
                rowDimensions: rowDimensions,
                columnDimensions: columnDimensions,
                rowSort: rowSort,
                columnSort: columnSort,
                limit: limit,
                offset: offset
        )
        hypercubeDataSerializationService.writeTable(Format.JSON, constraint, tableConfig, user, out)
    }

    /**
     * Cross table endpoint:
     * <code>/v2/observations/crosstable?rowConstraints=${rowConstraints}&columnConstraints=${columnConstraints}&
     * patientSetId=&{patientSetId}</code>
     *
     * Expects a list of {@link Constraint} <code>rowConstraints</code>
     * and a list of {@link Constraint} <code>columnConstraints</code>.
     *
     * The patientSetId should be the id of related set of patients.
     *
     * @return a tabular representation of counts in a json format.
     */
    def crosstable() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['rowConstraints', 'columnConstraints', 'patientSetId'])

        [rowConstraints: args.rowConstraints, columnConstraints: args.columnConstraints].each { name, list ->
            if (!list instanceof List || list.any { !it instanceof String }) {
                throw new InvalidArgumentsException("$name must be a JSON array of strings")
            }
        }
        List<Constraint> rowConstraints = args.rowConstraints.collect { constraint -> bindConstraint(constraint) }
        List<Constraint> columnConstraints = args.columnConstraints.collect { constraint -> bindConstraint(constraint.toString()) }

        if (args.patientSetId == null) {
            throw new InvalidArgumentsException("Parameter 'patientSetId' is required")
        }
        Long patientSetId = Long.parseLong((String) args.patientSetId)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        OutputStream out = getLazyOutputStream(Format.JSON)

        CrossTable crossTable = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, patientSetId, user)
        new CrossTableSerializer().write(crossTable.rows, out)
    }

    /**
     * Count endpoint:
     * <code>/v2/observations/counts?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a the number of observations that satisfy the constraint and the number of associated patients.
     */
    def counts() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def counts = aggregateDataResource.counts(constraint, user)
        render counts as JSON
    }

    /**
     * Count endpoint:
     * <code>/v2/observations/counts_per_concept?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map from concept code to the number of observations that satisfy the constraint for that concept.
     */
    def countsPerConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def counts = aggregateDataResource.countsPerConcept(constraint, user)
        def result = [countsPerConcept: counts]
        render result as JSON
    }

    /**
     * Count endpoint:
     * <code>/v2/observations/counts_per_study?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map from study if to the number of observations that satisfy the constraint for that study.
     */
    def countsPerStudy() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def counts = aggregateDataResource.countsPerStudy(constraint, user)
        def result = [countsPerStudy: counts]
        render result as JSON
    }

    /**
     * Count endpoint:
     * <code>/v2/observations/counts_per_study_and_concept?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map from study id to a map from concept code to the number of observations that satisfy the constraint
     * for that combination of study and concept.
     */
    def countsPerStudyAndConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def counts = aggregateDataResource.countsPerStudyAndConcept(constraint, user)
        counts.collectEntries { studyId, countsPerConcept ->
            [(studyId): [countsPerConcept: countsPerConcept]]
        }
        def result = [countsPerStudy: counts]
        render result as JSON
    }

    /**
     * Aggregate endpoint:
     * <code>/v2/observations/aggregates_per_concept?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter
     * parameter <code>constraint</code>.
     *
     * @return a map with the aggregates.
     */
    def aggregatesPerConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept = aggregateDataResource
                .numericalValueAggregatesPerConcept(constraint, user)
        Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept = aggregateDataResource
                .categoricalValueAggregatesPerConcept(constraint, user)

        Map resultMap = buildResultMap(numericalValueAggregatesPerConcept, categoricalValueAggregatesPerConcept)
        render resultMap as JSON
    }

    private static Map buildResultMap(Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept,
                                      Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept) {
        Set<String> foundConceptCodes = numericalValueAggregatesPerConcept.keySet() + categoricalValueAggregatesPerConcept.keySet()
        def aggregatesPerConcept = foundConceptCodes.collectEntries { String conceptCode ->
            Map<String, Object> valueAggregates = [:]
            NumericalValueAggregates numericalValueAggregates = numericalValueAggregatesPerConcept[conceptCode]
            if (numericalValueAggregates) {
                valueAggregates.numericalValueAggregates = numericalValueAggregates
            }
            CategoricalValueAggregates categoricalValueAggregates = categoricalValueAggregatesPerConcept[conceptCode]
            if (categoricalValueAggregates) {
                valueAggregates.categoricalValueAggregates = categoricalValueAggregates
            }
            [conceptCode, valueAggregates]
        }
        [ aggregatesPerConcept: aggregatesPerConcept ]
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
    private def highdimObservations(String type, String assay_constraint, String biomarker_constraint, projection) {

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        Constraint assayConstraint = getConstraintFromString(assay_constraint)

        BiomarkerConstraint biomarkerConstraint = biomarker_constraint ?
                (BiomarkerConstraint) getConstraintFromString(biomarker_constraint) : new BiomarkerConstraint()

        Format format = contentFormat
        OutputStream out = getLazyOutputStream(format)

        try {
            hypercubeDataSerializationService.writeHighdim(format, type, assayConstraint, biomarkerConstraint, projection, user, out)
        } finally {
            out.close()
        }
    }


    /**
     * Supported fields endpoint:
     * <code>/v2/supportedFields</code>
     *
     * @return the list of fields supported by {@link org.transmartproject.core.multidimquery.query.FieldConstraint}.
     */
    def supportedFields() {
        checkForUnsupportedParams(params, [])

        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }

    /**
     * Helper function to parse params of different types in GET and POST calls
     * This will no longer be needed after fixing getGetOrPostParams
     * TODO add a proper validation and error handling
     */
    private static Object parseIfJson(value){
        if(value instanceof ArrayList) {
            value.collect { parseIfJson(it) }
        } else {
            try {
                def result = parseJson(value)
                return result
            } catch (InvalidArgumentsException ex) {
                return value
            }
        }
    }
}
