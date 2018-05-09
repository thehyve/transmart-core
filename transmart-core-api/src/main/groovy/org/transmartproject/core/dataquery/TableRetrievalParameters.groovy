package org.transmartproject.core.dataquery

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.Dimension

import javax.validation.constraints.NotNull

@CompileStatic
class TableRetrievalParameters {

    DataRetrievalParameters dataRetrievalParameters

    List<Dimension> rowDimensions, columnDimensions

    Map<Dimension, SortOrder> sort, userSort

    Integer offset

    @NotNull
    Integer limit

}
