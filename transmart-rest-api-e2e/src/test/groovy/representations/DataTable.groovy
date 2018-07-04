package representations

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ColumnHeader {
    String dimension
    List<Object> elements
    List<Object> keys
}

@EqualsAndHashCode
class Dimension {
    String name
    Map<String, Object> elements
}

@EqualsAndHashCode
class RowHeader {
    String dimension
    Object element
    Object key
}

@EqualsAndHashCode
class Row {
    List<RowHeader> rowHeaders
    List<Object> cells
}

@EqualsAndHashCode
class SortSpecification {
    String dimension
    String sortOrder
    Boolean userRequested
}

@EqualsAndHashCode
class DataTable {
    List<ColumnHeader> columnHeaders
    List<Dimension> rowDimensions
    List<Dimension> columnDimensions
    Integer rowCount
    List<Row> rows
    Integer offset
    List<SortSpecification> sort
}
