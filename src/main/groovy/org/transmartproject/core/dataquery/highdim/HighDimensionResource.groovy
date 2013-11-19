package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult

/**
 * A resource that serves an entry point to obtain sub-resources allowing access
 * to certain types of high-dimensional data.
 */
public interface HighDimensionResource {

    /**
     * Obtains a sub-resource for doing operations on a certain type of data
     *
     * @param dataTypeName the string name with which the wanted data type is registered
     * @return The sub-resource that represents the requested data type
     * @throws NoSuchResourceException if the <code>dataTypeName</code> is not known.
     */
    HighDimensionDataTypeResource getSubResourceForType(String dataTypeName) throws NoSuchResourceException

    /**
     * Retrieves sub-resources for all the data types for which the given
     * patient set has data, along with the number of patients associated with
     * each data type
     *
     * @param queryResult the patient set. If you have a result instance id instead,
     * you will have to go through the {@link QueriesResource}.
     * @return A (possibly empty) map where the entries are the data type resources and
     * the values are the number of patients
     */
    Map<HighDimensionDataTypeResource, Long> getSubResourcesForPatientSet(QueryResult queryResult)
}
