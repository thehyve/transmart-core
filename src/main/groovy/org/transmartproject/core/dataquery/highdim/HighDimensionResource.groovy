package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult

/**
 * A resource that serves an entry point to obtain sub-resources allowing access
 * to certain types of high-dimensional data.
 */
public interface HighDimensionResource {

    /**
     * Returns the set of data type names that can be used to obtain
     * {@link HighDimensionDataTypeResource} objects with
     * {@link #getSubResourceForType(java.lang.String)}.
     *
     * @return set of known data type names
     */
    Set<String> getKnownTypes()

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
     * patient set has data, along with the assays for each sub-resource.
     *
     * @param queryResult the patient set. If you have a result instance id instead,
     * you will have to go through the {@link QueriesResource}.
     * @return A (possibly empty) map between the sub-resources and a collection of
     * assays (associated with the patients in the passed in patient set) for each
     * that sub-resource has data.
     */
    Map<HighDimensionDataTypeResource, Collection<Assay>> getSubResourcesAssayMultiMap(QueryResult queryResult)
}
