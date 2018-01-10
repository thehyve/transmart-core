package org.transmartproject.db.multidimquery

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.ValueFetchingDataColumn
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.util.AbstractGroupingIterator

/**
 * Constructs tabular result with specifying which dimensions will go to column and cell.
 * It's expected from the hypercube to be ordered by row dimensions.
 */
@Slf4j
@CompileStatic
class HypercubeTabularResultView implements TabularResult<HypercubeDataColumn, HypercubeDataRow> {

    public static final String DIMENSION_ELEMENTS_DELIMITER = ', '
    public static final String NO_DIMENSIONS_LABEL = 'Values'
    final Hypercube hypercube
    final Iterable<Dimension> rowDimensions
    final Iterable<Dimension> columnDimensions
    final Iterable<Dimension> cellDimensions
    final String columnsDimensionLabel
    final String rowsDimensionLabel

    HypercubeTabularResultView(final Hypercube hypercube,
                               final Iterable<Dimension> rowDimensions = [],
                               final Iterable<Dimension> columnDimensions = []) {
        this.hypercube = hypercube
        this.rowDimensions = rowDimensions
        this.columnDimensions = columnDimensions
        this.cellDimensions = hypercube.dimensions - rowDimensions - columnDimensions
        this.columnsDimensionLabel = getCoordinateLabel(columnDimensions)
        this.rowsDimensionLabel = getCoordinateLabel(rowDimensions)
        checkDimensionsConsistency()
    }

    List<HypercubeDataColumn> getIndicesList() {
        Set<Map<Dimension, Integer>> columnIndexes = []
        rows.forEachRemaining({ HypercubeDataRow row ->
            columnIndexes.addAll(row.presentColumnIndexes)
        })
        columnIndexes.collect { Map<Dimension, Integer> index ->
            new HypercubeDataColumn(index, hypercube)
        }
    }

    @Override
    Iterator<HypercubeDataRow> getRows() {
        iterator()
    }

    @Override
    void close() throws IOException {
        hypercube.close()
    }

    @Override
    Iterator<HypercubeDataRow> iterator() {
        new HypercubeTabularResultIterator(hypercube.iterator()) as Iterator<HypercubeDataRow>
    }

    private static String getCoordinateLabel(Iterable<Dimension> dimensions) {
        dimensions.collect { Dimension dim -> dim.name }.join(DIMENSION_ELEMENTS_DELIMITER) ?: NO_DIMENSIONS_LABEL
    }

    private static Map<Dimension, Integer> getIndex(Iterable<Dimension> dimensions, HypercubeValue hypercubeValue) {
        Map<Dimension, Integer> result = [:]
        for (Dimension dim: dimensions) {
            result.put(dim, hypercubeValue.getDimElementIndex(dim))
        }
        result
    }

    private checkDimensionsConsistency() {
        def notSupportedRowDimensions = rowDimensions - hypercube.dimensions as List<Dimension>
        assert !notSupportedRowDimensions: 'Following row dimensions are not supported by the hypercube: ' + notSupportedRowDimensions
        def notSupportedColumnDimensions = columnDimensions - hypercube.dimensions
        assert !notSupportedColumnDimensions: 'Following column dimensions are not supported by the hypercube: ' + notSupportedColumnDimensions
        def dimensionsIntersection = columnDimensions.intersect(cellDimensions)
        assert !dimensionsIntersection: 'Following dimensions found as row and value dimensions at the same time: ' + dimensionsIntersection
        assert (rowDimensions + columnDimensions + cellDimensions).containsAll(hypercube.dimensions)
    }

    @CompileStatic
    class HypercubeTabularResultIterator extends AbstractGroupingIterator<HypercubeDataRow, HypercubeValue, Map<Dimension, Integer>> {

        HypercubeTabularResultIterator(Iterator<HypercubeValue> iterator) {
            super(iterator)
        }

        @Override
        Map<Dimension, Integer> calculateGroupKey(HypercubeValue hValue) {
            getIndex(rowDimensions, hValue)
        }

        @Override
        HypercubeDataRow computeResultItem(Map<Dimension, Integer> groupKey, Iterable<HypercubeValue> groupedHValues) {
            def columnIndexToValue = [:]
            for (HypercubeValue hValue: groupedHValues) {
                def columnIndex = getIndex(columnDimensions, hValue)
                assert !columnIndexToValue.containsKey(columnIndex) :
                        "There is more then one hypercube value that falls to [${groupKey} : ${columnIndex}] table cell."
                columnIndexToValue[columnIndex] = hValue
            }
            new HypercubeDataRow(
                    groupKey,
                    columnIndexToValue)
        }
    }
}

@EqualsAndHashCode
@ToString
@CompileStatic
class HypercubeDataColumn<T> implements ValueFetchingDataColumn<Object, HypercubeDataRow> {
    final Map<Dimension, Integer> index
    final Hypercube hypercube

    HypercubeDataColumn(Map<Dimension, Integer> index, Hypercube hypercube) {
        this.index = index
        this.hypercube = hypercube
    }

    @Override
    String getLabel() {
        index.collect { dim, index ->  "${dim.name}: ${index}" }
                .join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
    }

    Object getDimensionElement(Dimension dimension) {
        assert index.containsKey(dimension)
        hypercube.dimensionElement(dimension, index[dimension])
    }

    @Override
    Object getValue(HypercubeDataRow row) {
        row.getHypercubeValue(index).value
    }
}

@EqualsAndHashCode
@ToString
@CompileStatic
class HypercubeDataRow<T> implements DataRow<ValueFetchingDataColumn, T> {
    final Map<Dimension, Integer> index
    private final Map<Map, HypercubeValue> columnIndexToHValue

    HypercubeDataRow(final Map<Dimension, Integer> index,
                     Map<Map, HypercubeValue> columnIndexToHValue) {
        this.index = index
        this.columnIndexToHValue = columnIndexToHValue
    }

    @Override
    String getLabel() {
        index.collect { dim, index ->  "${dim.name}: ${index}" }
                .join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
    }

    @Override
    T getAt(ValueFetchingDataColumn column) {
        column.getValue(this)
    }

    HypercubeValue getHypercubeValue(Map<Dimension, Integer> columnIndex) {
        columnIndexToHValue[columnIndex]
    }

    Set<Map<Dimension, Integer>> getPresentColumnIndexes() {
        columnIndexToHValue.keySet()
    }

    Object getDimensionElement(Dimension dimension) {
        assert index.containsKey(dimension)
        columnIndexToHValue.values().first()[dimension]
    }
}