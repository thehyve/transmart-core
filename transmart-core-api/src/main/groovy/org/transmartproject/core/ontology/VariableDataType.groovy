package org.transmartproject.core.ontology

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A data type of column values.
 */
enum VariableDataType {
    NUMERIC, DATE, DATETIME, STRING

    private static final Map<String, VariableDataType> mapping = new HashMap<>()
    static {
        for (VariableDataType type: values()) {
            mapping.put(type.name().toLowerCase(), type)
        }
    }

    @JsonCreator
    static VariableDataType forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            return null
        }
    }
}
