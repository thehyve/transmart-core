package org.transmartproject.core.dataquery.highdim.projections

/**
 * A projection determines that which data will show up in the result set's
 * cells.
 *
 * @param < CELL > The type of the cells in the
 * {@link org.transmartproject.core.dataquery.TabularResult}.
 */
public interface Projection<CELL> {

    /**
     * The data type default projection for getting real data series from the
     * API. If several real values are available, the most representative or
     * or least processed value should be returned.
     *
     * Data type implementations are strongly encouraged to support this
     * projection, if possible.
     */
    public static final DEFAULT_REAL_PROJECTION = 'default_real_projection'

    /**
     * Projection for getting the log transformation of original data points.
     *
     */
    public static final LOG_INTENSITY_PROJECTION = 'log_intensity'

    /**
     * Projection for getting transformed data points representing the standard
     * score of each original data point.
     *
     * It's implementation-defined whether this score is calculated on the
     * filtered data set (with rows or assays excluded) or on the complete
     * data set.
     */
    public static final ZSCORE_PROJECTION = 'zscore'

    /**
     * This generic projection returns a map with the values of all fields of
     * the datatype it is applied to.
     *
     * The object returned from HighDimensionResource.createProjection is of
     * type AllDataProjection. Its rowProperties property describes the
     * properties available on the rows and the dataProperties likewise
     * for the cell map values.
     */
    public static final ALL_DATA_PROJECTION = 'all_data'

    /**
     * This method takes an implementation-defined object and returns the final
     * cell value.
     * @param object an implementation-defined object
     * @return the final cell value
     */
    CELL doWithResult(Object object)

}
