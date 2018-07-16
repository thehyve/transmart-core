package org.transmartproject.rest.utils

import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONElement
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.users.User

@Slf4j
class ResponseEntityUtils {

    static <T> T toObject(ResponseEntity<Resource> response, Class<T> type) {
        new ObjectMapper().readValue(response.body.inputStream, type)
    }

    static String toString(ResponseEntity<Resource> response) {
        response.body.inputStream.readLines().join('\n')
    }

    static JSONElement toJson(ResponseEntity<?> response) {
        def body = response.body
        if (body instanceof JSONElement) {
            return (JSONElement) body
        } else if (body instanceof Resource) {
            return (JSONElement) JSON.parse(body.inputStream, 'UTF-8')
        }
    }

    static void checkResponseStatus(ResponseEntity<Resource> response, HttpStatus status, User user) {
        if (response.statusCode != status) {
            def message =
                    "Unexpected status ${response.statusCode} for user ${user.username}. " +
                            "${status.value()} expected.\n" +
                            "Response: ${toString(response)}"
            log.error message
            throw new UnexpectedResultException(message)
        }
    }
}
