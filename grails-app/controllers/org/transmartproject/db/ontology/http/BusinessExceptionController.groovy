package org.transmartproject.db.ontology.http

import grails.converters.JSON
import org.transmartproject.db.http.BusinessExceptionResolver

class BusinessExceptionController {

    def index() {
        Integer httpStatus = request.getAttribute(
                BusinessExceptionResolver.REQUEST_ATTRIBUTE_STATUS)
        Exception e = request.getAttribute(
                BusinessExceptionResolver.REQUEST_ATTRIBUTE_EXCEPTION)

        response.setStatus(httpStatus)

        render([
                httpStatus: httpStatus,
                type: e.getClass().simpleName,
                message: e.message
        ] as JSON)
    }
}
