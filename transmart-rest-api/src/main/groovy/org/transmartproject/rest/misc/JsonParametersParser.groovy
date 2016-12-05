package org.transmartproject.rest.misc

import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONElement
import org.grails.web.json.JSONObject
import org.transmartproject.core.exceptions.InvalidArgumentsException

class JsonParametersParser {
    public static Map<String, List> parseConstraints(String paramValue) {
        if (!paramValue) return [:]

        JSONElement constraintsElement
        try {
            constraintsElement = JSON.parse(paramValue)
        } catch (ConverterException ce) {
            throw new InvalidArgumentsException(
                    "Failed parsing as JSON: $paramValue", ce)
        } catch (StackOverflowError se) { // *sigh*
            throw new InvalidArgumentsException(
                    "Failed parsing as JSON: $paramValue", se)
        }

        if (!constraintsElement instanceof JSONObject) {
            throw new InvalidArgumentsException(
                    'Expected constraints to be JSON map')
        }

        // normalize [constraint_name: [ param1: foo ]] to
        //           [constraint_name: [[ param1: foo ]]]
        return ((JSONObject) constraintsElement).collectEntries { String constraintName, value ->
            if (!(value instanceof Map || value instanceof List)) {
                throw new InvalidArgumentsException(
                        "Invalid parameters for contraint $constraintName: " +
                                "$value (should be a list or a map)")
            } else if (value instanceof Map) {
                [constraintName, [value]]
            } else { // List
                [constraintName, value] // entry unchanged
            }
        }
    }
}
