package org.transmartproject.db.multidimquery

import groovy.transform.Immutable
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.TypeAwareDataColumn
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.util.AbstractGroupingIterator
import org.transmartproject.db.util.ClassUtils

/**
 * Constructs tabular result with specifying which dimensions will go to row or column.
 * It's expected from the hypercube to be ordered by row dimensions.
 */
@Slf4j
class HypercubeTabularResultView implements TabularResult<HypercubeDataColumn, HypercubeDataRow> {

    public static final String DIMENSION_ELEMENTS_DELIMITER = ' / '
    public static final String NO_DIMENSIONS_LABEL = 'Values'
    final Hypercube hypercube
    final Iterable<Dimension> rowDimensions
    final Iterable<Dimension> columnDimensions
    final String columnsDimensionLabel
    final String rowsDimensionLabel

    HypercubeTabularResultView(final Hypercube hypercube, final Iterable<Dimension> columnDimensions = []) {
        def notSupportedDimensions = columnDimensions - hypercube.dimensions
        assert !notSupportedDimensions: 'Following are not supported dimensions by the hypercube:' + notSupportedDimensions

        this.hypercube = hypercube
        this.columnDimensions = columnDimensions
        this.rowDimensions = hypercube.dimensions - columnDimensions
        this.columnsDimensionLabel = getCoordinateLabel(columnDimensions)
        this.rowsDimensionLabel = getCoordinateLabel(rowDimensions)
    }

    @Lazy
    List<HypercubeDataColumn> indicesList = {
        Map<Map<Dimension, Object>, Class> columnKeyToType = [:]
        rows.each { HypercubeDataRow row ->
            row.columnKeyToValue.each { key, value ->
                columnKeyToType[key] = ClassUtils.getCommonClass(columnKeyToType[key], value?.class)
            }
        }
        columnKeyToType.collect { Map<Dimension, Object> key, Class type ->
            new HypercubeDataColumn(
                    label: getLabel(columnDimensions, key),
                    key: key,
                    type: type
            )
        }.sort()
    }()

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
        new HypercubeTabularResultIterator(hypercube.iterator())
    }

    private static String getLabel(Iterable<Dimension> dimensions, Map<Dimension, Object> key) {
        dimensions.collect { Dimension dim -> key[dim] }.join(DIMENSION_ELEMENTS_DELIMITER) ?: 'value'
    }

    private static String getCoordinateLabel(Iterable<Dimension> dimensions) {
        dimensions*.name.join(DIMENSION_ELEMENTS_DELIMITER) ?: NO_DIMENSIONS_LABEL
    }

    private static Map<Dimension, Object> getKey(Iterable<Dimension> dimensions, HypercubeValue hypercubeValue) {
        dimensions.collectEntries { Dimension dim -> [dim, hypercubeValue.getDimKey(dim)] }
    }

    class HypercubeTabularResultIterator extends AbstractGroupingIterator<HypercubeDataRow, HypercubeValue> {

        private HypercubeTabularResultIterator(Iterator<HypercubeValue> iterator) {
            super(iterator)
        }

        @Override
        boolean isTheSameGroup(HypercubeValue hValue1, HypercubeValue hValue2) {
            getKey(rowDimensions, hValue1) == getKey(rowDimensions, hValue2)
        }

        @Override
        HypercubeDataRow computeResultItem(Iterable<HypercubeValue> groupedHValues) {
            def rowKey = groupedHValues ? getKey(rowDimensions, groupedHValues.first()) : [:]
            def columnKeyToValue = [:]
            groupedHValues.each { HypercubeValue hValue ->
                def columnKey = getKey(columnDimensions, hValue)
                def value = hValue.value

                assert !columnKeyToValue.containsKey(columnKey):
                        "There is more then one value in the table cell with " +
                                "\"${getLabel(rowDimensions, rowKey)}\" row and " +
                                "\"${getLabel(columnDimensions, columnKey)}\" column."

                columnKeyToValue[columnKey] = value
            }
            new HypercubeDataRow(
                    label: getLabel(rowDimensions, rowKey),
                    columnKeyToValue: columnKeyToValue,
                    key: rowKey)
        }
    }
}

@Immutable
@Sortable(includes = ['label'])
@ToString
class HypercubeDataColumn<T> implements TypeAwareDataColumn<T> {
    String label
    Map<Dimension, Object> key
    Class<T> type
}

@Immutable
@Sortable(includes = ['label'])
@ToString
class HypercubeDataRow implements DataRow<HypercubeDataColumn, Object> {
    String label
    Map<Dimension, Object> key
    Map<Map<Dimension, Object>, Object> columnKeyToValue

    @Override
    Object getAt(HypercubeDataColumn column) {
        columnKeyToValue[column.key]
    }
}