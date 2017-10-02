package org.transmartproject.core.dataquery

/**
 * Metadata about the column.
 */
class ColumnMetadata {
    ColumnDataType type
    Measure measure
    String description
    Integer width
    Integer decimals
    Integer columns
    Map<Integer, String> valueLabels = [:]
    List<Integer> missingValues = []
}
