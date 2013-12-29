package jobs.table

interface Column {

    /**
     * A name for the column.
     *
     * @return column name
     */
    String getColumnHeader()

    /**
     * What to do when a value is missing.
     *
     * @return
     */
    MissingValueAction getMissingValueAction()

    /**
     * Whether this column requires looking at the entire result set before it
     * can produce any data.
     *
     * @return whether this is a global computation column
     */
    boolean isGlobalComputation()

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
     * @return row key (e.g. patient id, or patient_id + sth else) -> column value
     */
    Map<String, String> consumeResultingTableRows()

    /**
     * Method called whenever a data source has been completely read.
     *
     * @param dataSourceName
     */
    void onDataSourceDepleted(String dataSourceName, Iterable dataSource, BackingMap backingMap)

    /**
     * Method called just before the data source starts being iterated
     *
     * @param dataSourceName then name of the data source
     * @param dataSource the data source object
     */
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource)

}
