/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.rest.Link
import grails.rest.render.util.AbstractLinkingRenderer
import grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.*
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.QueryResultWrapper
import org.transmartproject.rest.user.AuthContext

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.READ

/**
 * Exposes patient set resources.
 */
class PatientSetController {

    static responseFormats = ['json', 'hal']

    private final static String VERSION = 'v1'

    @Autowired
    private QueriesResource queriesResource

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    QueryDefinitionXmlConverter queryDefinitionXmlConverter

    @Autowired
    AuthContext authContext

    /**
     * Not yet supported in core-api.
     *
     * GET /v1/patient_sets
     */
    def index() {
        List result = queriesResource.getQueryResultsSummaryByUsername(authContext.user.username)
        respond wrapPatients(result)
    }

    /**
     * Show details of a patient set
     *
     * GET /v1/patient_sets/<result_instance_id>
     */
    def show(Long id) {
        QueryResult queryResult = queriesResource.getQueryResultFromId(id)

        if (!authorisationChecks.canPerform(authContext.user, READ, queryResult)) {
            throw new AccessDeniedException()
        }

        respond new QueryResultWrapper(
                apiVersion: VERSION,
                queryResult: queryResult,
                embedPatients: true
        )
    }

    /**
     * Create a new patient set.
     *
     * POST /v1/patient_sets
     */
    def save() {
        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)

        if (!(mimeType.name in [MimeType.XML.name, MimeType.TEXT_XML.name])) {
            throw new InvalidRequestException("Content type should been " +
                    "text/xml or application/xml; got $mimeType")
        }

        QueryDefinition queryDefinition =
                queryDefinitionXmlConverter.fromXml(request.reader)

        if (!authorisationChecks.canPerform(authContext.user, READ, queryDefinition)) {
            throw new AccessDeniedException()
        }

        respond new QueryResultWrapper(
                apiVersion: VERSION,
                queryResult: queriesResource.runQuery(queryDefinition, authContext.user),
                embedPatients: true
        ),
                [status: 201]
    }

    /**
     * Disable created patient set.
     *
     * DELETE /patient_sets/<result_instance_id>
     */
    def disable(Long id) {
        queriesResource.disablingQuery(id, authContext.user.username)
        respond status: 204
    }

    private wrapQueryResultSummary(Object source) {
        new ContainerResponseWrapper
                (
                        container: source,
                        componentType: QueryResultSummary,
                        links: [new Link(AbstractLinkingRenderer.RELATIONSHIP_SELF, "$VERSION/patient_sets")]
                )
    }

    private wrapPatients(Object source) {
        new ContainerResponseWrapper(
                key: 'subjects',
                container: source,
                componentType: Patient,
                links: [new Link(AbstractLinkingRenderer.RELATIONSHIP_SELF, "/patient_sets")]
        )
    }
}
