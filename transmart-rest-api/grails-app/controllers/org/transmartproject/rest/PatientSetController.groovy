package org.transmartproject.rest

import grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryDefinitionXmlConverter
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.rest.misc.CurrentUser

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.BUILD_COHORT
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.READ

/**
 * Exposes patient set resources.
 */
class PatientSetController {

    static responseFormats = ['json', 'hal']

    @Autowired
    private QueriesResource queriesResource

    @Autowired
    QueryDefinitionXmlConverter queryDefinitionXmlConverter

    @Autowired
    CurrentUser currentUser

    /**
     * Not yet supported in core-api.
     *
     * GET /patient_sets
     */
    def index() {
        throw new NoSuchResourceException('Listing previously created ' +
                'patient resources is not yet possible')
    }

    /**
     * Show details of a patient set
     *
     * GET /patient_sets/<result_instance_id>
     */
    def show(Long id) {
        QueryResult queryResult = queriesResource.getQueryResultFromId(id)

        currentUser.checkAccess(READ, queryResult)

        respond queryResult
    }

    /**
     * Create a new patient set.
     *
     * POST /patient_sets
     */
    def save() {
        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)

        if (!(mimeType in [MimeType.XML, MimeType.TEXT_XML])) {
            throw new InvalidRequestException("Content type should been " +
                    "text/xml or application/xml; got $mimeType")
        }

        QueryDefinition queryDefinition =
                queryDefinitionXmlConverter.fromXml(request.reader)

        currentUser.checkAccess(BUILD_COHORT, queryDefinition)

        respond queriesResource.runQuery(queryDefinition, currentUser.username),
                [status: 201]
    }
}
