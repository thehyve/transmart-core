/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.exceptions.*
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.SortSpecification
import org.transmartproject.core.multidimquery.aggregates.AggregatesPerCategoricalConcept
import org.transmartproject.core.multidimquery.aggregates.AggregatesPerNumericalConcept
import org.transmartproject.core.multidimquery.aggregates.CategoricalValueAggregates
import org.transmartproject.core.multidimquery.aggregates.NumericalValueAggregates
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.datatable.PaginationParameters
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.multidimquery.export.Format
import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.DimensionMetadata
import org.transmartproject.rest.misc.LazyOutputStreamDecorator

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams
import static org.transmartproject.rest.misc.RequestUtils.parseJson

@Slf4j
class QueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    @Autowired
    AggregateDataResource aggregateDataResource

    @Autowired
    CrossTableResource crossTableResource

    @Autowired
    DataTableViewDataSerializationService dataTableViewDataSerializationService

    @Value('${org.transmartproject.patientCountThreshold}')
    long patientCountThreshold

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

        def sort = BindingHelper.readList(sort_text, sortListTypeReference)

        OutputStream out = getLazyOutputStream(format)

        try {
            def args = new DataRetrievalParameters(constraint: constraint, sort: sort)
            hypercubeDataSerializationService.writeClinical(format, args, authContext.user, out)
        } catch (LegacyStudyException e) {
            throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
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
     * <code>POST /v2/observations/table</code>
     * Body:
     * <code>{
     *      "type": ${type},
     *      "constraint": ${constraint},
     *      "rowDimensions": ${rowDimensions},
     *      "columnDimensions": ${columnDimensions},
     *      "rowSort": ${rowSort},
     *      "columnSort": ${columnSort},
     *      "limit": ${limit},
     *      "offset": ${offset}
     * }</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     * Only 'clinical' type is currently supported.
     * Expects columnDimensions and rowDimensions parameters.
     * Optional rowSort and columnSort parameters allow to define the sorting.
     * Pagination is supported via limit and offset parameters.
     *
     * @return a tabular representation of hypercube in JSON format.
     */
    def table() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['type', 'constraint', 'rowDimensions', 'columnDimensions',
                                         'rowSort', 'columnSort', 'limit', 'offset'])

        if ((!args.type)) {
            throw new InvalidArgumentsException("No data type provided.")
        }
        if (args.type != 'clinical') {
            throw new OperationNotImplementedException("High dimensional data is not yet " +
                "implemented for the data table")
        }

        Constraint constraint = bindConstraint((String) args.constraint)

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

        def tableConfig = new TableConfig(
                rowDimensions: rowDimensions,
                columnDimensions: columnDimensions,
                rowSort: rowSort,
                columnSort: columnSort
        )
        BindingHelper.validate(tableConfig)
        def pagination = new PaginationParameters(
                limit: limit,
                offset: offset
        )
        BindingHelper.validate(pagination)
        dataTableViewDataSerializationService.writeTablePageToJson(constraint, tableConfig, pagination, authContext.user, out)
    }

    /**
     * Cross table endpoint:
     * <code>POST /v2/observations/crosstable</code>
     * Body:
     * <code>{
     *      "rowConstraints": ${rowConstraints},
     *      "columnConstraints": ${columnConstraints},
     *      "subjectConstraint": {subjectConstraint}
 *      }</code>
     *
     * Expects a list of {@link Constraint} <code>rowConstraints</code>
     * and a list of {@link Constraint} <code>columnConstraints</code>.
     *
     * Expects a {@link Constraint} <code>subjectConstraint</code> as a constraints for a related set of patients.
     * In particular, subjectConstraint can be of type {@link org.transmartproject.core.multidimquery.query.PatientSetConstraint}
     * in order to explicitly specify the id of the set of patients.
     *
     * @return a tabular representation of counts in JSON format.
     */
    def crosstable() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['rowConstraints', 'columnConstraints', 'subjectConstraint'])

        [rowConstraints: args.rowConstraints, columnConstraints: args.columnConstraints].each { name, list ->
            if (!list instanceof List || list.any { !it instanceof String }) {
                throw new InvalidArgumentsException("$name must be a JSON array of strings")
            }
        }
        List<Constraint> rowConstraints = args.rowConstraints.collect { constraint -> bindConstraint((String) constraint) }
        List<Constraint> columnConstraints = args.columnConstraints.collect { constraint -> bindConstraint((String) constraint) }
        Constraint subjectConstraint = bindConstraint((String) args.subjectConstraint)

        CrossTable crossTable = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint,
                authContext.user)
        respond crossTable
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
        def counts = aggregateDataResource.counts(constraint, authContext.user)
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
        def counts = aggregateDataResource.countsPerConcept(constraint, authContext.user)
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
        def counts = aggregateDataResource.countsPerStudy(constraint, authContext.user)
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
        def counts = aggregateDataResource.countsPerStudyAndConcept(constraint, authContext.user)
        counts.collectEntries { studyId, countsPerConcept ->
            [(studyId): [countsPerConcept: countsPerConcept]]
        }
        def result = [countsPerStudy: counts]
        render result as JSON
    }

    /**
     * Count threshold endpoint:
     * <code>/v2/patient_counts_threshold</code>
     *
     * @return a threshold value, below which counts are not available for users
     * with `COUNTS_WITH_THRESHOLD` access permission.
     */
    def countsThreshold() {
        checkForUnsupportedParams(params, [])

        def result = [threshold: patientCountThreshold]
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
    @Deprecated
    def aggregatesPerConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept = aggregateDataResource
                .numericalValueAggregatesPerConcept(constraint, authContext.user)
        Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept = aggregateDataResource
                .categoricalValueAggregatesPerConcept(constraint, authContext.user)

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

    def aggregatesPerNumericalConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept = aggregateDataResource
                .numericalValueAggregatesPerConcept(constraint, authContext.user)

        def aggregatesPerNumericalConcept = new AggregatesPerNumericalConcept(numericalValueAggregatesPerConcept)
        writeAsJsonToOutputStream(aggregatesPerNumericalConcept)
    }

    def aggregatesPerCategoricalConcept() {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept = aggregateDataResource
                .categoricalValueAggregatesPerConcept(constraint, authContext.user)

        def aggregatesPerCategoricalConcept = new AggregatesPerCategoricalConcept(categoricalValueAggregatesPerConcept)
        writeAsJsonToOutputStream(aggregatesPerCategoricalConcept)
    }

    protected void writeAsJsonToOutputStream(Object result) {
        response.contentType = 'application/json'
        response.characterEncoding = 'utf-8'
        new ObjectMapper().writeValue(response.outputStream, result)
        response.status = HttpStatus.OK.value()
        response.outputStream.flush()
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

        Constraint assayConstraint = getConstraintFromString(assay_constraint)

        BiomarkerConstraint biomarkerConstraint = biomarker_constraint ?
                (BiomarkerConstraint) getConstraintFromString(biomarker_constraint) : new BiomarkerConstraint()

        Format format = contentFormat
        OutputStream out = getLazyOutputStream(format)

        hypercubeDataSerializationService.writeHighdim(format, type, assayConstraint, biomarkerConstraint, projection,
                authContext.user, out)
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
