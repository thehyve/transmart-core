package org.transmartproject.core.multidimquery

import com.fasterxml.jackson.annotation.JsonCreator
import groovy.transform.CompileStatic

@CompileStatic
enum SortOrder {
    ASC,
    DESC,
    NONE

    String string() {
        name().toLowerCase()
    }

    private static final Map<String, SortOrder> mapping = new HashMap<>()
    static {
        for (SortOrder order : values()) {
            mapping.put(order.name().toLowerCase(), order)
        }
    }

    @JsonCreator
    static SortOrder forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            return NONE
        }
    }

}
