package selectors

interface Selector {

    /**
     * returns a value of a specific field
     *
     * @param cellIndex
     * @param dimension
     * @param fieldName
     * @param valueType one of "Double", "String", "Int", "Timestamp"
     * @return
     */
    Object select(int cellIndex, String dimension, String fieldName, String valueType)

    /**
     *
     * @param cellIndex
     * @param dimension
     * @param valueType
     * @return
     */
    Object select(int cellIndex, String dimension, String valueType)

    /**
     * returns the value of a cell
     *
     * @param cellIndex
     * @return the value of a cell
     */
    Object select(int cellIndex)

}