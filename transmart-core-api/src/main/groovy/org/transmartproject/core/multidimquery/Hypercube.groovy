/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.dimensions.Order

/**
 * A Hypercube represents a result dataset. Conceptually a hypercube is an N-dimensional table, so a table with
 * (potentially) more than two dimensions. The values in the hypercube are the observations in the dataset, The
 * dimensions (i.e. what would be rows or columns in a simple two-dimensional flat table) represent for example
 * patients, concepts, studies, visits, etc.
 *
 * The hypercube has a number of dimensions which have names such as 'patients', concepts', etc, and each dimension
 * has elements, which could be the individual patient objects, or concept paths, or study objects, etc. The former
 * is referred to as 'dimensions', the latter as 'dimension elements'
 *
 * The existing implementations of Hypercube will stream their values when you use the {@Link #iterator()} method.
 * Dimension elements for sparse dimensions are available immediately. For dimesion elements of dense dimensions the
 * Hypercube will remember which elements are part of this dataset. Only when you access such an element will the
 * dimension elements for that dimension be loaded from the database. It is therefore the most efficient to first
 * retrieve all dimension values and only after that access a dense dimension's elements, in that way all the elements
 * for a single dense dimension can be retrieved in a single operation. If you access dimension elements each time after
 * retrieving a single value, that could result in a lot of inefficient database queries for single elements. To
 * identify dimension elements without loading them use the {@Link #dimensionElementKey} and
 * {@Link HypercubeValue#getDimElementIndex} methods.
 *
 * The dimension elements for sparse dimensions usually do not require additional database queries to retrieve, and
 * also they are not remembered by the Hypercube, so you will need to retrieve them directly from the HypercubeValues.
 *
 * Some implementations also support loading all applicable dense dimension elements before all values have been
 * retrieved. This is referred to as 'dimension preloading'. Preloading will be more expensive than only accessing
 * dimension elements after all values have been retieved, but it can be more efficient than accessing dimension
 * elements after each value is retrieved, which can cause dimension elements to be retrieved one by one.
 */
interface Hypercube extends IterableResult<HypercubeValue> {

    /**
     * The most important method, calling iterator() returns an iterator with the actual values
     * @return
     */
    PeekingIterator<HypercubeValue> iterator()

    /**
     * The list of dimensions that are part of this hypercube. Dimensions might be patients, concepts, visits, etc.
     * Studies themselves are also a dimension, and some properties of observations such as start time, end time, etc
     * are also represented as dimensions.
     */
    ImmutableList<Dimension> getDimensions()

    /**
     * This call returns a list of elements for a specified dimension. 'Elements' in this context refer to, for
     * example for the patient dimension, the individual patients, or the individual concepts for the concept dimension.
     *
     * Only elements of dimensions that are dense (See {@Link Dimension#getDensity} can be retrieved in this way. For
     * sparse dimensions use the {@Link HypercubeValue#getAt} or {@Link HypercubeValue#getDimKey} methods directly.
     *
     * To access the properties of a dimension element (i.e. a patient's age, id, or the name of a concept), see the
     * {@Link Dimension#getElementFields} method and the {@Link Property} interface.
     *
     * @param dim The Dimension for which to retrieve the elements. This must be dense
     * @return a list of dimension elements.
     */
    ImmutableList<Object> dimensionElements(Dimension dim)

    /**
     * An ordered map that includes the dimensions on which the result is ordered. The order in the map is the same
     * as the order in which the sorting is applied to the result. The values indicate if the sorting is ascending or
     * descending.
     */
    ImmutableMap<Dimension, Order> getSorting()

    /**
     * Equivalent to dimensionElements(dim).get(idx), but may be a bit more efficient if you don't need all of the
     * elements. Only works for dense dimensions
     */
    Object dimensionElement(Dimension dim, Integer idx)

    /**
     * This method returns the 'key' for a dimension element. A key is a simple value, a number, String, or Date,
     * that identifies the dimension element. This can be used to identify a dimension element without the overhead
     * of the full element.
     *
     * Calling this method will not cause additional database queries.
     *
     * @param dim The dimension, which must be dense.
     * @param idx The index of the dimension element in the list returned from {@Link #dimensionElements}
     * @return The key for a dimension element
     */
    Object dimensionElementKey(Dimension dim, Integer idx)

    /**
     * Return the largest index value that is currently used for this index. This maximum will increase if new
     * elements for this dimension are encountered while streaming results, unless the dimensions are preloaded. In
     * that case the maximum index will be fixed.
     *
     * If no indexes have been assigned for this dimension, returns -1.
     *
     * @param dim the dimension to check for. Must be a dense dimension.
     * @return the largest index value currently assigned, or -1 if no indexes have been assigned yet
     */
    int maximumIndex(Dimension dim)

    /**
     * Returns true if dimensions can be preloaded.
     */
    boolean isDimensionsPreloadable()

    /**
     * Load all dimensions used in this dataset. If dimensionsPreloadable == false this method throws
     * UnsupportedOperationException
     *
     * @see #loadDimensions()
     */
    void preloadDimensions()

    /**
     * @return True if dimensions have been preloaded
     */
    boolean isDimensionsPreloaded()

    /**
     * If true (default), dimensions will be automatically loaded once the values iterator is exhausted. For some
     * implementations this property does not do anything.
     */
    boolean isAutoloadDimensions()
    void setAutoloadDimensions(boolean autoload)

    /**
     * Loads all dimension elements for the currently known dimension keys. This method differs from
     * {@Link #preloadDimensions} in that this method will only load the dimension elements to which the values that
     * have been retrieved up to now belong to. preloadDimensions() (if supported) will load all dimension elements
     * that are part of this Hypercube, including those whose values have not yet been seen.
     */
    void loadDimensions()
}
