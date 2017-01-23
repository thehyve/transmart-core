package jobs.table

interface Column {

    /**
     * A name for the column.
     *
     * @return column name
     */
    String getHeader()

    /**
     * What to do when a value is missing.
     *
     * @return
     */
    MissingValueAction getMissingValueAction()

    /**
     * Method called whenever a new row is read.
     *
     * This method will probably need to store some data to then be retrieved
     * through {@link #consumeResultingTableRows()}.
     *
     * @param dataSourceName the name of the data source from which the row was
     * read
     * @param row the row read from the data source
     */
    void onReadRow(String dataSourceName, Object row)

    /**
     * Consume the entries that have been stored after having read an
     * unspecified number of rows.
     *
     * Unless the object needs the returned data to generate further rows, it
     * can discard the returned data.
     *
     * Do not return null values! Omit the entry instead.
     *
     * @return row key (e.g. patient id, or patient_id + sth else) -> column value
     */
    Map<String, Object> consumeResultingTableRows()

    /**
     * Method called whenever a data source has been completely read.
     *
     * @param dataSourceName
     */
    void onDataSourceDepleted(String dataSourceName, Iterable dataSource)

    /**
     * Method called when all the tables' data sources (and not just the ones
     * this column subscribes to) are exhausted.
     *
     * @param columnNumber
     * @param backingMap
     */
    void onAllDataSourcesDepleted(int columnNumber, BackingMap backingMap)

    /**
     * Method called just before the data source starts being iterated
     *
     * @param dataSourceName then name of the data source
     * @param dataSource the data source object
     */
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource)

    /**
     * A transformation to apply to the values returned by
     * {@link Table#getResult()} or null.
     *
     * @return a closure taking a {@link org.mapdb.Fun.Tuple3}&lt;String, Integer,String>
     *         and the original value and returning an arbitrary object
     */
    Closure<Object> getValueTransformer()

}
