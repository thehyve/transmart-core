package org.transmartproject.core.multidimquery.export

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.multidimquery.query.Constraint

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@CompileStatic
/**
 * {@link org.transmartproject.core.multidimquery.DataRetrievalParameters}
 */
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
