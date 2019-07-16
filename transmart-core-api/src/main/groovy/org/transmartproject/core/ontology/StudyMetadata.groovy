package org.transmartproject.core.ontology

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.CompileStatic

/**
 * Metadata about the study.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
class StudyMetadata {

    Map<String, VariableMetadata> conceptCodeToVariableMetadata

    @JsonSetter('conceptCodeToVariableMetadata')
    setConceptCodeToVariableMetadata(Map<String, VariableMetadata> conceptCodeToVariableMetadata) {
        this.conceptCodeToVariableMetadata = conceptCodeToVariableMetadata
        for (VariableMetadata metadata in conceptCodeToVariableMetadata.values()) {
            // Fix the type of the missing values for numeric variables
            if (metadata.type == VariableDataType.NUMERIC && metadata.missingValues) {
                metadata.missingValues.values = metadata.missingValues.values.collect {
                    Object value -> value as BigDecimal
                } as List<Object>
            }
        }
    }

}
