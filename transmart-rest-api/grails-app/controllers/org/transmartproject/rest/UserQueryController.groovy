package org.transmartproject.rest

import com.fasterxml.jackson.core.JsonProcessingException
import grails.converters.JSON
import grails.web.mime.MimeType
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.binding.BindingException
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.userquery.UserQueryResource
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.rest.user.AuthContext

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class UserQueryController {

    static responseFormats = ['json']

    @Autowired
    VersionController versionController

    @Autowired
    AuthContext authContext

    @Autowired
    UserQueryResource userQueryResource

    @Autowired
    UserQuerySetResource userQuerySetResource

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    LegacyAuthorisationChecks legacyAuthorisationChecks

    @Autowired
    ConceptsResource conceptsResource

    /**
     * GET /v2/queries
     *
     * @returns the list of all queries saved by the user.
     */
    def index() {
        def queries = userQueryResource.list(authContext.user)
        respond queries: queries
    }

    /**
     * GET /v2/queries/{id}
     *
     * @param id the id of the saved query
     * @returns the saved query with the provided id, if it exists and is owned by the user,
     *      status 404 (Not Found) or 403 (Forbidden) otherwise.
     */
    def get(@PathVariable('id') Long id) {
        checkForUnsupportedParams(params, ['id'])
        def query = userQueryResource.get(id, authContext.user)
        respond query
    }

    protected static UserQueryRepresentation getUserQueryFromString(String src) {
        if (src == null || src.trim().empty) {
            throw new InvalidArgumentsException('Empty user query.')
        }
        try {
            try {
                UserQueryRepresentation userQuery = BindingHelper.objectMapper.readValue(src, UserQueryRepresentation.class)
                BindingHelper.validate(userQuery)
                userQuery
            } catch (JsonProcessingException e) {
                throw new BindingException("Cannot parse user query parameter: ${e.message}", e)
            }
        } catch (ConverterException c) {
            throw new InvalidArgumentsException('Cannot parse user query parameter', c)
        }
    }

    /**
     * Deserialises the request body to a user query representation object using Jackson.
     *
     * Uses a serialised version of request.JSON, because using request.inputStream
     * directly prevents the {@link org.transmartproject.interceptors.ApiAuditInterceptor}
     * from reading the request body.
     *
     * @returns the user query representation object is deserialisation was successful;
     * responds with code 400 and returns null otherwise.
     */
    protected UserQueryRepresentation bindUserQuery() {
        if (!request.contentType) {
            throw new InvalidRequestException('No content type provided')
        }
        MimeType mimeType = new MimeType(request.contentType)
        if (mimeType != MimeType.JSON) {
            throw new InvalidRequestException("Content type should be ${MimeType.JSON.name}; got ${mimeType}.")
        }

        try {
            def src = BindingHelper.objectMapper.writeValueAsString(request.JSON)
            return getUserQueryFromString(src)
        } catch (BindingException e) {
            def error = [
                    httpStatus: 400,
                    message   : e.message,
                    type      : e.class.simpleName,
            ] as Map<String, Object>

            if (e.errors) {
                error.errors = e.errors
                        .collect { [propertyPath: it.propertyPath.toString(), message: it.message] }
            }

            response.status = 400
            render error as JSON
            return null
        }
    }

    /**
     * POST /v2/queries
     * Saves the user query in the body, which is of type {@link UserQueryRepresentation}.
     *
     * @param apiVersion
     * @returns a representation of the saved query.
     */
    def save(@RequestParam('api_version') String apiVersion) {
        UserQueryRepresentation body = bindUserQuery()
        if (body == null) {
            return
        }
        body.apiVersion = versionController.currentVersion(apiVersion)
        def query = userQueryResource.create(body, authContext.user)
        response.status = 201
        respond query
    }

    /**
     * PUT /v2/queries/{id}
     * Saves changes to an existing user query.
     * Changes are specified in the body, which is of type {@link UserQueryRepresentation}.
     *
     * @param apiVersion
     * @param id the identifier of the user query to update.
     * @returns status 204 and the updated query object, if it exists and is owned by the current user;
     *      404 (Not Found) or 403 (Forbidden) otherwise.
     */
    def update(@RequestParam('api_version') String apiVersion,
               @PathVariable('id') Long id) {
        UserQueryRepresentation body = bindUserQuery()
        if (body == null) {
            return
        }
        def query = userQueryResource.update(id, body, authContext.user)
        response.status = 204
        respond query
    }

    /**
     * DELETE /v2/queries/{id}
     * Deletes the user query with the provided id.
     *
     * @param id the database id of the user query.
     * @returns status 204, if the user query exists and is owned by the current user;
     *      404 (Not Found) or 403 (Forbidden) otherwise.
     */
    def delete(@PathVariable('id') Long id) {
        userQueryResource.delete(id, authContext.user)
        response.status = 204
    }

}
