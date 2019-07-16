package org.transmartproject.core.ontology

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Levels of measurement. Used in the SPSS export.
 */
enum Measure {
    NOMINAL, ORDINAL, SCALE

    private static final Map<String, Measure> mapping = new HashMap<>()
    static {
        for (Measure measure: values()) {
            mapping.put(measure.name().toLowerCase(), measure)
        }
    }

    @JsonCreator
    static Measure forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            return null
        }
    }
}
