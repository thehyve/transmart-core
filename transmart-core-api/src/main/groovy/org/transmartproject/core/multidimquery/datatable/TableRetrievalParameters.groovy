package org.transmartproject.core.multidimquery.datatable

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.SortOrder
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.hypercube.Dimension

@CompileStatic
class TableRetrievalParameters {

    DataRetrievalParameters dataRetrievalParameters

    List<Dimension> rowDimensions, columnDimensions

    Map<Dimension, SortOrder> sort, userSort

}
