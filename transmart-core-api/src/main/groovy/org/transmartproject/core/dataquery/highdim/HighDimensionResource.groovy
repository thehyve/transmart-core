package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException

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
     * Finds all the assays that match the given assay constraints and returns
     * a map mapping the data type sub-resources to their respective assays,
     * where these assays are those found in the first step.
     *
     * @param assayConstraints the assay constraints
     * @return A (possibly empty) map between the sub-resources and a collection of
     * assays (associated with the patients in the passed in patient set) for each
     * that sub-resource has data.
     */
    Map<HighDimensionDataTypeResource, Collection<Assay>> getSubResourcesAssayMultiMap(
            List<AssayConstraint> assayConstraints)

    /**
     * Creates an assay constraint for use in {@link #getSubResourceForType(String)}.
     *
     * The supported constraints are those listed as constants in
     * {@link AssayConstraint}.
     *
     * @param params the constraint parameters
     * @param name the name of the constraint
     * @return the constraint
     * @throws InvalidArgumentsException if the parameters are inappropriate for
     * the constraint or the constraint name is invalid
     */
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name)
}
