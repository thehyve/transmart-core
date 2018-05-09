package org.transmartproject.rest.serialization

import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.multidimquery.query.Constraint

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@CompileStatic
class ExportJobRepresentation {

    @Valid
    @NotNull
    Constraint constraint

    @Valid
    @NotNull
    @Size(min = 1)
    List<ExportElement> elements

    Boolean includeMeasurementDateColumns

    @Valid
    TableConfig tableConfig

}
