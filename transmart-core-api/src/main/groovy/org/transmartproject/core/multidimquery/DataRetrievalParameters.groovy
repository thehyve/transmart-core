package org.transmartproject.core.multidimquery

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.SortSpecification
import org.transmartproject.core.multidimquery.query.Constraint

@CompileStatic
class DataRetrievalParameters {

    Constraint constraint

    List<String> dimensions

    List<SortSpecification> sort

    Boolean includeMeasurementDateColumns

    String exportFileName

}
