package org.transmartproject.core.dataquery.constraints

import groovy.transform.Canonical
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.querytool.QueryResult

/**
 * Query constraints that are common to high dimensional data queries.
 */
@Canonical
class CommonHighDimensionalQueryConstraints {

    List<String> studies

    QueryResult patientQueryResult

    /* Limitations on the assays: */

    List<Platform> platforms

    List<String> sampleCodes

    List<String> tissueCodes

    List<String> timepointCodes

}
