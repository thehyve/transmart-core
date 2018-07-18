package org.transmartproject.core.querytool

import org.transmartproject.core.dataquery.Patient

/**
 * A query result instance contains information about a specific run of a
 * query.
 *
 * All query results are assumed to be of PATIENTSET type.
 */
interface QueryResult extends QueryResultSummary {

    /**
     * A description of the query, set by the creator.
     *
     * @return the textual description of the query.
     */
    String getDescription()

    /**
     * The set of patients included in this result.
     * @deprecated Query result is not necessarily associated with patients.
     * @return the set of patients
     */
    @Deprecated
    Set<Patient> getPatients()

    /**
     * Information on the type of the result query produced.
     * @return the query result type
     */
    QueryResultType getQueryResultType()

}
