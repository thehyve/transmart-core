/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import com.google.common.base.Function
import com.google.common.collect.Iterators
import grails.converters.JSON
import grails.web.mime.MimeType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.PatientWrapper
import org.transmartproject.rest.marshallers.QueryResultWrapper

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class PatientQueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal']

    /**
     * Patients endpoint:
     * <code>/v2/patients?constraint=${constraint}</code>
     *
     * Expects a {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.PatientDimension} objects for
     * which there are observations that satisfy the constraint.
     */
    def listPatients(@RequestParam('api_version') String apiVersion) {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        def patients = multiDimService.getDimensionElements(multiDimService.getDimension('patient'), constraint, authContext.user)
        respond wrapPatients(apiVersion, patients)
    }

    /**
     * Patients endpoint:
     * <code>/v2/patients/${id}</code>
     *
     * @param id the patient id
     *
     * @return the {@link org.transmartproject.db.i2b2data.PatientDimension} object with id ${id}
     * if it exists and the user has access; null otherwise.
     */
    def findPatient(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('id') Long id) {
        if (id == null) {
            throw new InvalidArgumentsException("Parameter 'id' is missing.")
        }

        checkForUnsupportedParams(params, ['id'])

        Constraint constraint = new PatientSetConstraint(patientIds: [id])
        def patient = multiDimService.getDimensionElements(multiDimService.getDimension('patient'),
                constraint, authContext.user)[0]
        if (patient == null) {
            throw new NoSuchResourceException("Patient not found with id ${id}.")
        }

        respond new PatientWrapper(
                patient: patient,
                apiVersion: apiVersion
        )
    }

    private def wrapPatients(String apiVersion, Iterable<Patient> source) {
        new ContainerResponseWrapper(
                key: 'patients',
                container: Iterators.transform(source.iterator(),
                        { new PatientWrapper(apiVersion: apiVersion, patient: it) } as Function),
                componentType: Patient,
        )
    }

    /**
     * Patient set endpoint:
     * <code>GET /v2/patient_sets/${id}</code>
     *
     * Finds the patient set ({@link org.transmartproject.core.querytool.QueryResult}) with result instance id <code>id</code>.
     *
     * @return a map with the query result id, size and status.
     */
    def findPatientSet(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('id') Long id) {
        checkForUnsupportedParams(params, ['id'])

        QueryResult patientSet = patientSetResource.findQueryResult(id, authContext.user)
        def version = patientSet.queryInstance.queryMaster.apiVersion
        def constraintText = patientSet.queryInstance.queryMaster.requestConstraints

        render new QueryResultWrapper(
                apiVersion: version,
                queryResult: patientSet,
                requestConstraint: constraintText
        ) as JSON
    }

    /**
     * Patient sets endpoint:
     * <code>GET /v2/patient_sets</code>
     *
     * Finds all the patient sets that User has access to.
     *
     * @return a list of maps with the query result id, size and status.
     */
    def findPatientSets(
            @RequestParam('api_version') String apiVersion) {
        checkForUnsupportedParams(params, [])

        Iterable<QueryResult> patientSets = patientSetResource.findPatientSetQueryResults(authContext.user)

        respond wrapPatientSets(patientSets)
    }

    /**
     * Patient set creation endpoint:
     * <code>POST /v2/patient_sets?name=${name}&reuse=${reuse}</code>
     *
     * Creates a patient set ({@link org.transmartproject.core.querytool.QueryResult}) based on
     * the request body of type {@link Constraint}.
     *
     * @param apiVersion
     * @param name
     * @param reuse (default: false)
     *
     * @return a map with the query result id, description, size, status, constraints and api version.
     */
    def createPatientSet(
            @RequestParam('api_version') String apiVersion,
            @RequestParam('name') String name,
            @RequestParam('reuse') Boolean reuse) {
        if (name) {
            name = URLDecoder.decode(name, 'UTF-8').trim()
        } else {
            throw new InvalidArgumentsException("Parameter 'name' is missing.")
        }
        if (name.empty) {
            throw new InvalidArgumentsException("Empty 'name' parameter.")
        }

        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)
        if (mimeType != MimeType.JSON) {
            throw new InvalidRequestException("Content type should be " +
                    "${MimeType.JSON.name}; got ${mimeType}.")
        }

        // FIXME: we now expect a plain constraint in the body, this should be wrapped in a {"constraint": ...} wrapper
        // for consistency with other calls
        def src = BindingHelper.objectMapper.writeValueAsString(request.JSON)
        Constraint constraint = getConstraintFromString(src)
        if (constraint == null) {
            throw new InvalidArgumentsException("No valid constraint in the body.")
        }

        checkForUnsupportedParams(params, ['name', 'constraint', 'reuse'])

        String currentVersion = VersionController.currentVersion(apiVersion)

        // Canonise the constraint, to enable reuse
        constraint = constraint.canonise()

        if (reuse == null) {
            reuse = false
        }

        QueryResult patientSet = patientSetResource.createPatientSetQueryResult(name, constraint, authContext.user,
                currentVersion, reuse)

        response.status = 201
        render new QueryResultWrapper(
                apiVersion: currentVersion,
                queryResult: patientSet,
                requestConstraint: constraint.toJson()
        ) as JSON
    }

    private def wrapPatientSets(Iterable<QueryResult> source) {
        new ContainerResponseWrapper(
                key: 'patientSets',
                container: source.collect {
                    new QueryResultWrapper(
                            apiVersion: it.queryInstance.queryMaster.apiVersion,
                            queryResult: it,
                            requestConstraint: it.queryInstance.queryMaster.requestConstraints
                    )
                },
                componentType: QueryResult,
        )
    }

}
