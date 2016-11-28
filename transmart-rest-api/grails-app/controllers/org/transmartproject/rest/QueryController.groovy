package org.transmartproject.rest

import grails.converters.JSON
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.UsersResource
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser
import org.transmartproject.rest.misc.LazyOutputStreamDecorator
import org.transmartproject.rest.protobuf.ObservationsSerializer

@Slf4j
class QueryController {

    static responseFormats = ['json', 'hal', 'protobuf']

    @Autowired
    QueryService queryService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    @Autowired
    MultidimensionalDataSerialisationService multidimensionalDataSerialisationService

    def conceptsResourceService

    HighDimensionResourceService highDimensionResourceService


    private Constraint parseConstraint(String constraintText) {
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

    private Constraint getConstraint(String constraintParameterName = 'constraint') {
        if (!params.containsKey(constraintParameterName)) {
            throw new InvalidArgumentsException("${constraintParameterName} parameter is missing.")
        }
        if (!params[constraintParameterName]) {
            throw new InvalidArgumentsException('Empty constraint parameter.')
        }
        String constraintParam = URLDecoder.decode(params[constraintParameterName], 'UTF-8')
        parseConstraint(constraintParam)
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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
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
        ObservationsSerializer.Format format = getContentFormat()
        if (format == ObservationsSerializer.Format.NONE) {
            throw new InvalidArgumentsException("Format not supported.")
        }

        Constraint constraint = bindConstraint()
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Hypercube result = queryService.retrieveClinicalData(constraint, user)

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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def patients = queryService.listPatients(constraint, user)
        render patients as JSON
    }

    /**
     * Patient set endpoint:
     * <code>POST /v2/patient_set?constraint=${constraint}</code>
     *
     * Creates a patient set ({@link QueryResult}) based the {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map with key 'id' and the id of the resulting {@link QueryResult} as value.
     */
    def patientSet() {
        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)
        if (mimeType != MimeType.JSON) {
            throw new InvalidRequestException("Content type should be " +
                    "${MimeType.JSON.name}; got ${mimeType}.")
        }
        Constraint constraint = parseConstraint(request.reader.lines().iterator().join(''))
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        QueryResult patientSet = queryService.createPatientSet("test set", constraint, user)
        def result = [id: patientSet.id] as Map
        response.status = 201
        render result as JSON
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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
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
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def aggregatedValue = queryService.aggregate(aggregateType, constraint, user)
        def result = [(aggregateType.name().toLowerCase()): aggregatedValue]
        render result as JSON
    }

    ObservationsSerializer.Format getContentFormat() {
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

        ObservationsSerializer.Format format = getContentFormat()
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
     * <code>/query/supportedFields</code>
     *
     * @return the list of fields supported by {@link org.transmartproject.db.multidimquery.query.FieldConstraint}.
     */
    def supportedFields() {
        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }

}
