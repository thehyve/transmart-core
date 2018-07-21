/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.rest.http.BusinessExceptionResolver
import org.transmartproject.rest.user.AuthContext

class BusinessExceptionController {

    @Autowired
    AccessLogEntryResource accessLogEntryResource

    @Autowired
    AuthContext authContext

    protected String getIp() {
        return request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
    }

    protected String getUrl() {
        return "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
    }

    protected String getEventMessage(Throwable throwable) {
        Map<String, Object> message = [
                ip     : (Object)ip,
                action : (Object)actionName,
                status : (Object)response.status.toInteger(),
                message: (Object)throwable.message
        ]
        if (request.isPost()) {
            try {
                message.put("body", request.JSON as Map)
            } catch (IllegalStateException e) {
                log.error "Cannot read body for request ${url}: ${e.message}" +
                        "\nTry to use request.inputStream instead of request.reader."
            }
        }
        return BindingHelper.objectMapper.writeValueAsString(message)
    }

    def index() {
        Integer httpStatus = (Integer)request.getAttribute(
                BusinessExceptionResolver.REQUEST_ATTRIBUTE_STATUS)
        Exception e = (Exception)request.getAttribute(
                BusinessExceptionResolver.REQUEST_ATTRIBUTE_EXCEPTION)

        response.setStatus(httpStatus)

        accessLogEntryResource.report(
                authContext.user,
                'error',
                eventMessage: (Object)getEventMessage(e),
                requestURL: (Object)url)

        render([
                httpStatus: httpStatus,
                type      : e.getClass().simpleName,
                message   : e.message
        ] as JSON)
    }
}
