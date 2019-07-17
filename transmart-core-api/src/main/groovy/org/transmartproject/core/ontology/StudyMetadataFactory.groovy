package org.transmartproject.core.ontology

import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingHelper

@CompileStatic
class StudyMetadataFactory {

    /**
     * Read study metadata from JSON.
     * @param src JSON text
     * @return the StudyMetadata object
     */
    static StudyMetadata read(String src) {
        BindingHelper.objectMapper.readValue(src, StudyMetadata)
    }

}
