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

package org.transmartproject.db.dataquery.highdim.parameterproducers

import com.google.common.collect.Iterables
import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Class with static methods to help the simple validation/binding done in
 * parameter producers.
 */
class BindingUtils {

    static void validateParameterNames(List<String> parameterNames,
                                       Map<String, Object> params) {
        validateParameterNames(parameterNames as Set, params)
    }

    static void validateParameterNames(Set<String> parameterNames,
                                       Map<String, Object> params) {
        def missingParameters = parameterNames - params.keySet()
        if (missingParameters) {
            if (missingParameters.size() == 1) {
                throw new InvalidArgumentsException('Missing required parameter "' +
                        Iterables.getFirst(missingParameters, null) + '"; got ' +
                        "the following parameters instead: ${params.keySet()}")
            } else {
                throw new InvalidArgumentsException('Missing several ' +
                        "required parameters: $missingParameters; " +
                        "got ${params.keySet()}")
            }
        }
        def extraParameters = params.keySet() - parameterNames
        if (extraParameters) {
            throw new InvalidArgumentsException("Unrecognized parameters: " +
                    "$extraParameters; only these are allowed: $parameterNames")
        }
    }

    static <T> T getParam(Map params, String paramName, Class<T> type) {
        def result = params[paramName]

        if (result == null) {
            throw new InvalidArgumentsException("The parameter $paramName is not in map $params")
        }


        if (!type.isAssignableFrom(result.getClass())) {
            throw new InvalidArgumentsException("Expected parameter $paramName to be of type $type; " +
                    "got class ${result.getClass()}")
        }

        result
    }

    static Long convertToLong(String paramName, Object obj) {
        if (obj instanceof Number) {
            obj = obj.longValue()
        } else if (obj instanceof String && obj.isLong()) {
            obj = obj.toLong()
        } else {
            throw new InvalidArgumentsException("Invalid value for $paramName: $obj")
        }
        obj
    }

    static List processList(String paramName, Object obj, Closure closure) {
        if (!(obj instanceof List)) {
            throw new InvalidArgumentsException("Parameter '$paramName' " +
                    "is not a List, got a ${obj.getClass()}")
        }

        if (obj.isEmpty()) {
            throw new InvalidArgumentsException('Value of parameter ' +
                    "'$paramName' is an empty list; this is unacceptable")
        }

        obj.collect { closure.call it }
    }

    static List<String> processStringList(String paramName, Object obj) {
        processList paramName, obj, {
            if (it instanceof String) {
                it
            } else if (it instanceof Number) {
                it.toString()
            } else {
                throw new InvalidArgumentsException("Parameter '$paramName' " +
                        "is not a list of String; found in a list an object with " +
                        "type ${it.getClass()}")
            }
        }
    }

    static List<Long> processLongList(String paramName, Object obj) {
        processList paramName, obj, {
            if (it instanceof String) {
                if (!it.isLong()) {
                    throw new InvalidArgumentsException("Parameter '$paramName' " +
                            "is not a list of longs; found in a list an object " +
                            "with type ${it.getClass()}")
                } else {
                    it as Long
                }
            } else if (it instanceof Number) {
                ((Number) it).longValue()
            } else {
                throw new InvalidArgumentsException("Parameter '$paramName' " +
                        "is not a list of longs; found in a list an object " +
                        "with type ${it.getClass()}")
            }
        }
    }

}
