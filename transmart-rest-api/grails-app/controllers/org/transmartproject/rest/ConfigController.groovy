package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.RuntimeConfigRepresentation
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.db.support.SystemService
import org.transmartproject.rest.misc.AuthContext

import javax.validation.Valid
import javax.validation.Validator

class ConfigController {

    static responseFormats = ['json']

    @Autowired
    AuthContext authContext

    @Autowired
    SystemService systemService

    @Autowired
    Validator validator


    private boolean validate(Object object, String message) {
        def errors = validator.validate(object)
        if (!errors.empty) {
            response.status = 400
            respond httpStatus: 400, message: message, errors: errors.collect { "${it.propertyPath} ${it.message}"}
            return false
        }
        true
    }

    /**
     * GET /v2/config
     *
     * Fetches the runtime config.
     *
     * @return the runtime config as JSON.
     * @throws org.transmartproject.core.exceptions.AccessDeniedException
     * if the current user is not admin.
     */
    def index() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        respond systemService.getRuntimeConfig()
    }

    /**
     * PUT /v2/config
     *
     * Updates the runtime config
     *
     * @param config the desired configuration.
     * @return updated runtime config as JSON.
     * @throws org.transmartproject.core.exceptions.AccessDeniedException
     * if the current user is not admin.
     */
    def update(@Valid RuntimeConfigRepresentation config) {
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        if (!validate(config, 'Invalid configuration')) {
            return
        }
        respond systemService.updateRuntimeConfig(config)
    }

}
