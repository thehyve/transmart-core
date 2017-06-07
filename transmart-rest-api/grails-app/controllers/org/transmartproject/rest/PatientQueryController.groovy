/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.PatientWrapper
import org.transmartproject.rest.marshallers.QueryResultWrapper

class PatientQueryController extends AbstractQueryController {

    @Autowired
    VersionController versionController

    static responseFormats = ['json', 'hal']

    /**
     * Patients endpoint:
     * <code>/v2/patients?constraint=${constraint}</code>
     *
     * Expects a {@link org.transmartproject.db.multidimquery.query.Constraint} parameter <code>constraint</code>.
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.PatientDimension} objects for
     * which there are observations that satisfy the constraint.
     */
    def listPatients(@RequestParam('api_version') String apiVersion) {
        def args = getGetOrPostParams()
        checkParams(args, ['constraint'])

        Constraint constraint = bindConstraint(args.constraint)
        if (constraint == null) {
            return
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def patients = multiDimService.listPatients(constraint, user)
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

        checkParams(params, ['id'])

        Constraint constraint = new PatientSetConstraint(patientIds: [id])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def patients = multiDimService.listPatients(constraint, user)
        if (patients.empty) {
            throw new NoSuchResourceException("Patient not found with id ${id}.")
        }

        respond new PatientWrapper(
                patient: patients[0],
                apiVersion: apiVersion
        )
    }

    private def wrapPatients(String apiVersion, Collection<Patient> source) {
        new ContainerResponseWrapper(
                key: 'patients',
                container: source.collect { new PatientWrapper(apiVersion: apiVersion, patient: it) },
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
        checkParams(params, ['id'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        QueryResult patientSet = multiDimService.findPatientSet(id, user)
        def constraint_version = multiDimService.getPatientSetConstraint(patientSet.id)

        render new QueryResultWrapper(
                apiVersion: constraint_version.constraint,
                queryResult: patientSet,
                requestConstraint: constraint_version.version
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
    def findAllPatientSets(
            @RequestParam('api_version') String apiVersion) {
        checkParams(params, [])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        List <QueryResult> patientSets = multiDimService.findAllPatientSets(user)

        respond wrapPatientSets(patientSets)
    }

    /**
     * Patient set creation endpoint:
     * <code>POST /v2/patient_sets?constraint=${constraint}&name=${name}</code>
     *
     * Creates a patient set ({@link org.transmartproject.core.querytool.QueryResult}) based the {@link Constraint} parameter <code>constraint</code>.
     *
     * @return a map with the query result id, description, size, status, constraints and api version.
     */
    def createPatientSet(
            @RequestParam('api_version') String apiVersion,
            @RequestParam('name') String name) {
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

        def bodyJson = request.JSON
        log.debug "body JSON: $bodyJson"

        // FIXME: we now expect a plain constraint in the body, this should be wrapped in a {"constraint": ...} wrapper
        // for consistency with other calls
        Constraint constraint = getConstraintFromStringOrJson(bodyJson)
        if (constraint == null) {
            return null
        }

        checkParams(params, ['name', 'constraint'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        
        String currentVersion = versionController.currentVersion(apiVersion)

        // This converts bodyJson back to string, but the request doesn't save the body, it only provides an
        // inputstream.
        QueryResult patientSet = multiDimService.createPatientSet(name, constraint, user, bodyJson.toString(), currentVersion)

        response.status = 201
        render new QueryResultWrapper(
                apiVersion: currentVersion,
                queryResult: patientSet,
                requestConstraint: bodyJson
        ) as JSON
    }

    private def wrapPatientSets(Collection<QueryResult> source) {
        new ContainerResponseWrapper(
                key: 'patientSets',
                container: source.collect {
                    def constraint_version = multiDimService.getPatientSetConstraint(it.id)
                    new QueryResultWrapper(
                            apiVersion: constraint_version.version,
                            queryResult: it,
                            requestConstraint: constraint_version.constraint
                    )
                },
                componentType: QueryResult,
        )
    }

}
