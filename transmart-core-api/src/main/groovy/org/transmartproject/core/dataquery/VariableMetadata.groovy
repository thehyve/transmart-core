package org.transmartproject.core.dataquery

/**
 * Metadata about the variable/column taken from SPSS.
 */
class VariableMetadata {
    /**
     * Data type of the concept
     */
    VariableDataType type
    /**
     * Levels of measurement
     */
    Measure measure
    /**
     * Description of the variable
     */
    String description
    /**
     * Refers to how many characters a value can hold
     */
    Integer width
    /**
     * Representation. Number of digits in fractional part.
     */
    Integer decimals
    /**
     * How many columns to represent on the screen.
     */
    Integer columns
    /**
     * Values index. e.g. 1=Female, 2=Male -1=Not provided
     */
    Map<Integer, String> valueLabels = [:]
    /**
     * Contains indexes that represent missing values. Usually negative values. e.g. -1
     */
    List<Integer> missingValues = []
}
