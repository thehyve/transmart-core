package org.transmartproject.rest.serialization

import com.fasterxml.jackson.core.type.TypeReference
import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingHelper

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@CompileStatic
class ExportElement {

    @NotNull
    Format format

    @NotNull
    @Size(min = 1)
    String dataType

    @Size(min = 1)
    String dataView

    static final TypeReference<List<ExportElement>> exportElementListTypeReference =
            new TypeReference<List<ExportElement>>(){}

    /**
     * Create a list of export element objects from a JSON string
     * using Jackson and validates the export elements.
     *
     * @param src the JSON string
     * @return a list of validated export elements
     */
    static List<ExportElement> read(String src) {
        BindingHelper.readList(src, exportElementListTypeReference)
    }

}
