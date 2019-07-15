package org.transmartproject.core.ontology

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.CompileStatic

@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
class MissingValues {
    BigDecimal lower
    BigDecimal upper
    List<Object> values = []

    @JsonSetter('value')
    setValue(Object value) {
        values.add(value)
    }
}
