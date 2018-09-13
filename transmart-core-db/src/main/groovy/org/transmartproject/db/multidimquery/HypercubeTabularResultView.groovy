package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableMap
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.ValueFetchingDataColumn
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.util.AbstractGroupingIterator

/**
 * Constructs tabular result with specifying which dimensions will go to column and cell.
 * It's expected from the hypercube to be ordered by row dimensions.
 */
@Slf4j
@CompileStatic
class HypercubeTabularResultView implements TabularResult<ValueFetchingDataColumn, HypercubeDataRow> {

    public static final String DIMENSION_ELEMENTS_DELIMITER = ', '
    public static final String NO_DIMENSIONS_LABEL = 'Values'
    final Hypercube hypercube
    final Iterable<Dimension> rowDimensions
    final Iterable<Dimension> columnDimensions
    final Iterable<Dimension> cellDimensions
    final String columnsDimensionLabel
    final String rowsDimensionLabel
    List<ValueFetchingDataColumn> indicesList

    private HypercubeTabularResultView(final Hypercube hypercube,
                               final Iterable<Dimension> rowDimensions,
                               final Iterable<Dimension> columnDimensions) {
        this.hypercube = hypercube
        this.rowDimensions = rowDimensions
        this.columnDimensions = columnDimensions
        this.cellDimensions = hypercube.dimensions - rowDimensions - columnDimensions
        this.columnsDimensionLabel = getCoordinateLabel(columnDimensions)
        this.rowsDimensionLabel = getCoordinateLabel(rowDimensions)
        checkDimensionsConsistency()
    }

    HypercubeTabularResultView(final Hypercube hypercube,
                               final Iterable<Dimension> rowDimensions,
                               final Iterable<Dimension> columnDimensions,
                               final List<ValueFetchingDataColumn> indicesList) {
        this(hypercube, rowDimensions, columnDimensions)
        this.indicesList = indicesList
    }

    @Override
    List<ValueFetchingDataColumn> getIndicesList() {
        indicesList
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

    private static ImmutableMap<Dimension, Object> getCoordinates(Iterable<Dimension> dimensions, HypercubeValue hypercubeValue) {
        ImmutableMap.Builder<Dimension, Object> result = ImmutableMap.builder()
        for (Dimension dim: dimensions) {
            result.put(dim, hypercubeValue.getDimKey(dim))
        }
        result.build()
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
    class HypercubeTabularResultIterator extends AbstractGroupingIterator<HypercubeDataRow, HypercubeValue, ImmutableMap<Dimension, Object>> {

        HypercubeTabularResultIterator(Iterator<HypercubeValue> iterator) {
            super(iterator)
        }

        @Override
        ImmutableMap<Dimension, Object> calculateGroupKey(HypercubeValue hValue) {
            getCoordinates(rowDimensions, hValue)
        }

        @Override
        HypercubeDataRow computeResultItem(ImmutableMap<Dimension, Object> groupKey, Iterable<HypercubeValue> groupedHValues) {
            Map<ImmutableMap, HypercubeValue> columnIndexToValue = [:]
            for (HypercubeValue hValue: groupedHValues) {
                def columnCoordinates = getCoordinates(columnDimensions, hValue)
                assert !columnIndexToValue.containsKey(columnCoordinates) :
                        "There is more then one hypercube value that falls to [${groupKey} : ${columnCoordinates}] table cell."
                columnIndexToValue[columnCoordinates] = hValue
            }
            new HypercubeDataRow(
                    groupKey,
                    columnIndexToValue)
        }
    }
}

/**
 * The hypercube data column represents
 */
@Canonical
@CompileStatic
class HypercubeDataColumn implements ValueFetchingDataColumn<Object, HypercubeDataRow> {
    final ImmutableMap<Dimension, Object> coordinates

    @Override
    String getLabel() {
        coordinates.collect { dim, value ->  "${dim.name}: ${value}" }
                .join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
    }

    @Override
    Object getValue(HypercubeDataRow row) {
        row.getHypercubeValue(coordinates)?.value
    }
}

@Canonical
@CompileStatic
class HypercubeDataRow<T> implements DataRow<ValueFetchingDataColumn, T> {
    final ImmutableMap<Dimension, Object> index
    final Map<ImmutableMap, HypercubeValue> columnIndexToHValue

    @Override
    String getLabel() {
        index.collect { dim, key ->  "${dim.name}: ${key}" }
                .join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
    }

    @Override
    T getAt(ValueFetchingDataColumn column) {
        column.getValue(this)
    }

    HypercubeValue getHypercubeValue(ImmutableMap<Dimension, Object> columnCoordinates) {
        columnIndexToHValue[columnCoordinates]
    }

    Object getDimensionElement(Dimension dimension) {
        assert index.containsKey(dimension)
        columnIndexToHValue.values().first()[dimension]
    }
}
