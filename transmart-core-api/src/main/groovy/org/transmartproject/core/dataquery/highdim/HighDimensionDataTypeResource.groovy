package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueryResult

/**
 *
 * @param < R >
 */
interface HighDimensionDataTypeResource<R extends DataRow<AssayColumn, ? /* depends on projection */>> {

    /**
     * The name with which this data type is registered.
     *
     * @return data type string name
     * @see HighDimensionResource#getSubResourceForType(java.lang.String)
     */
    String getDataTypeName()

    /**
     * A human-readable description of this data type
     */
    String getDataTypeDescription()

    Set<OntologyTerm> getAllOntologyTermsForDataTypeBy(QueryResult queryResult)

    /**
     * Retrieves high dimensional data from the database.
     *
     * The result is a matrix-like structure where the columns represent
     * assays and the rows represent an entity specific to the data type.
     *
     * Each column has associated an {@link AssayColumn} object; each row an
     * object of a type that is data type specific, but that in any case will
     * extend {@link DataRow}. Both objects have a 'label' property that be
     * used to identify a specific row or column, for instance for display
     * purposes.
     *
     * The results are provided row-by-row. There is no way to get beforehand
     * all the {@link DataRow} objects or even to know how many the result set
     * will contain.
     *
     * This method throws an {@link EmptySetException} exception if and only if
     * the assay constraints do not satisfy any assay. Other circumstances will
     * not result in an {@link EmptySetException} exception.
     *
     * @param assayConstraints list of constraints that filter the columns/
     * assays that are to be included in the result set
     * @param dataConstraints list of constraints that filter the rows that are
     * to be included in the result sets
     * @param projection defines what data goes into each cell. For instance, for
     * mRNA, one could want to retrieve the raw intensities or some form of
     * normalized data.
     * @return the request result set
     * @throws EmptySetException iif no assays can be found
     */
    TabularResult<AssayColumn, R> retrieveData(List<AssayConstraint> assayConstraints,
                                                 List<DataConstraint> dataConstraints,
                                                 Projection projection)

    /**
     * The list of {@link AssayConstraint} types supported by this data type,
     * identified by their name.
     * Only constraints of this type can by passed to this resource's
     * {@link #retrieveData(List, List, Projection)}.
     *
     *
     * @return set of supported assay constraints
     */
    Set<String> getSupportedAssayConstraints()

    /**
     * The list of {@link DataConstraint} types supported by this data type,
     * identified by their name/
     *
     * @return set of supported data constraints
     */
    Set<String> getSupportedDataConstraints()

    /**
     * The list of {@link Projection} types supported by this data type,
     * identified by their name.
     *
     * @return set of supported projections
     */
    Set<String> getSupportedProjections()

    /**
     * Create an {@link AssayConstraint} appropriate for this data type given
     * the provided abstract representation.
     *
     * @param name   the name of the constraint
     * @param params the parameters for the constraint; strings, maps (with
     *               string keys) and lists are supported
     * @return       the constraint
     * @throws UnsupportedByDataTypeException if the constraint name is not recognized
     * @throws InvalidArgumentsException      if the parameters are inappropriate for the constraint
     */
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name) throws UnsupportedByDataTypeException

    /**
     * Create a {@link DataConstraint} appropriate for this data type given
     * the provided abstract representation.
     *
     * @param name   the name of the constraint
     * @param params the parameters for the constraint; strings, maps (with
     *               string keys) and lists are supported
     * @return       the constraint
     * @throws UnsupportedByDataTypeException if the constraint name is not recognized
     * @throws InvalidArgumentsException      if the parameters are inappropriate for the constraint
     */
    DataConstraint createDataConstraint(Map<String, Object> params, String name) throws UnsupportedByDataTypeException

    /**
     * Create a {@link Projection} appropriate for this data type given
     * the provided abstract representation.
     *
     * @param name   the name of the constraint
     * @param params the parameters for the constraint; strings, maps (with
     *               string keys) and lists are supported
     * @return       the constraint
     * @throws UnsupportedByDataTypeException if the constraint name is not recognized
     * @throws InvalidArgumentsException      if the parameters are inappropriate for the constraint
     */
    Projection createProjection(Map<String, Object> params, String name) throws UnsupportedByDataTypeException

    /**
     * Overload for an empty params map
     * @throws UnsupportedByDataTypeException
     */
    Projection createProjection(String name) throws UnsupportedByDataTypeException

    /**
     * Whether the platform passed in refers to data of the type implemented by
     * this resource.
     *
     * Used to match assays (via their platforms) to data types.
     *
     * @param platform the platform to match
     * @return true iif the the platform and this data type match
     */
    boolean matchesPlatform(Platform platform)
}
