package org.transmartproject.db.multidimquery

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import spock.lang.Specification

class HypercubeTabularResultViewSpec extends Specification {

    def hypercube = Mock(Hypercube)
    def dim1 = Mock(Dimension)
    def dim2 = Mock(Dimension)
    def dim3 = Mock(Dimension)
    def var = Mock(Dimension)
    def dimensions = [dim1, dim2, dim3, var]
    def val1 = Mock(HypercubeValue)
    def val2 = Mock(HypercubeValue)
    def val3 = Mock(HypercubeValue)
    def val4 = Mock(HypercubeValue)
    def hypercubeValues = [val1, val2, val3, val4]

    def setup() {
        dim1.name >> 'dim1'
        dim2.name >> 'dim2'
        dim3.name >> 'dim3'
        var.name >> 'var'

        val1.value >> 'val 1'
        val1.getDimKey(dim1) >> 'dim1key1'
        val1.getDimKey(dim2) >> 'dim2key1'
        val1.getDimKey(dim3) >> 'dim3key1'
        val1.getDimKey(var) >> 'str_var_1'
        val2.value >> 2.0
        val2.getDimKey(dim1) >> 'dim1key2'
        val2.getDimKey(dim2) >> 'dim2key2'
        val2.getDimKey(dim3) >> 'dim3key2'
        val2.getDimKey(var) >> 'num_var_1'
        val3.value >> 'val 3'
        val3.getDimKey(dim1) >> 'dim1key2'
        val3.getDimKey(dim2) >> 'dim2key2'
        val3.getDimKey(dim3) >> 'dim3key2'
        val3.getDimKey(var) >> 'str_var_2'
        val4.value >> 4.0
        val4.getDimKey(dim1) >> 'dim1key4'
        val4.getDimKey(dim2) >> 'dim2key3'
        val4.getDimKey(dim3) >> 'dim3key2'
        val4.getDimKey(var) >> 'num_var_2'

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { hypercubeValues.iterator() }
    }

    def 'no column dimensions'() {
        when: 'no column dimensions provided'
        def view = new HypercubeTabularResultView(hypercube)
        then: 'all dimensions are row dimensions'
        view.columnsDimensionLabel == HypercubeTabularResultView.NO_DIMENSIONS_LABEL
        view.columnDimensions.size() == 0
        view.rowsDimensionLabel == dimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.rowDimensions == dimensions

        when: 'columns are read'
        List<HypercubeDataColumn> columns = view.indicesList
        then: 'there is only one value column'
        columns.size() == 1
        columns.first().label == 'value'
        then: 'common type is casted to object'
        columns.first().type == Object

        when: 'rows are read'
        List<HypercubeDataRow> rows = view.rows.toList()
        then: 'row labels composed from the dimension element keys'
        rows*.label == hypercubeValues.collect { value ->
            dimensions.collect { dim ->
                value.getDimKey(dim)
            }.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        }
        then: 'rows content matches hypercube values respecting order'
        rows.collect { row -> row[columns[0]] } == hypercubeValues*.value
    }

    def 'no row dimensions'() {
        when: 'all dimensions provided as column dimensions'
        def view = new HypercubeTabularResultView(hypercube, dimensions)
        then: 'there are no row dimensions'
        view.columnsDimensionLabel == dimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.columnDimensions == dimensions
        view.rowsDimensionLabel == HypercubeTabularResultView.NO_DIMENSIONS_LABEL
        view.rowDimensions.size() == 0

        when: 'columns are read'
        List<HypercubeDataColumn> columns = view.indicesList
        then: 'there are as many columns as values'
        columns.size() == hypercubeValues.size()
        columns*.label == hypercubeValues.collect { value ->
            dimensions.collect { dim ->
                value.getDimKey(dim)
            }.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        }
        then: 'column type is detected based on the value type'
        columns*.type == hypercubeValues.collect { it.value.class }

        when: 'rows are read'
        List<HypercubeDataRow> rows = view.rows.toList()
        then: 'row labels are values'
        rows.size() == 1
        rows.first().label == 'value'
        then: 'the row values match in regards to the indices order'
        columns.collect { column -> rows[0][column] } == hypercubeValues*.value
    }

    def 'row and column dimensions'() {
        def columnDimensions = [var, dim2]
        def rowDimensions = [dim1, dim3]

        when: 'column dimensions provided'
        def view = new HypercubeTabularResultView(hypercube, columnDimensions)
        then: 'column dimensions and row dimensions slitted with respect to the their order'
        view.columnDimensions == columnDimensions
        view.columnsDimensionLabel == columnDimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.rowDimensions == rowDimensions
        view.rowsDimensionLabel == rowDimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)

        when: 'columns are read'
        List<HypercubeDataColumn> columns = view.indicesList
        then: 'there are only columns for column dimensions'
        columns*.label == hypercubeValues.collect { value ->
            columnDimensions.collect { dim ->
                value.getDimKey(dim)
            }.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        }.unique().sort()

        when: 'rows are read'
        List<HypercubeDataRow> rows = view.rows.toList()
        then: 'row labels calculated based on the row dimensions with respect to the dimensions order'
        rows*.label == hypercubeValues.collect { value ->
            rowDimensions.collect { dim ->
                value.getDimKey(dim)
            }.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        }.unique()
    }

    def 'not existing column dimension'() {
        when: 'not existing column dimension provided'
        new HypercubeTabularResultView(hypercube, [Mock(Dimension, name: 'non-existent')])
        then: 'assert exception is thrown'
        def errorMessage = thrown(AssertionError)
        errorMessage.message.startsWith 'Following are not supported dimensions by the hypercube:'
        errorMessage.message.contains('non-existent')
    }

    def 'hypercube is closed'() {
        when: 'view is closed'
        new HypercubeTabularResultView(hypercube).close()
        then: 'hypercube is closed'
        with(hypercube) {
            1 * close()
        }
    }

    def 'two ways to get rows iterator'() {
        def view = new HypercubeTabularResultView(hypercube)

        when: 'rows are read'
        List<HypercubeDataRow> rows = view.rows.toList()
        then: 'rows are the same as returned by the iterator.'
        rows == view.toList()
    }

    def 'values collision'() {
        def hypercube = Mock(Hypercube)
        def dim1 = Mock(Dimension)
        def dim2 = Mock(Dimension)
        def dimensions = [dim1, dim2]
        def val1 = Mock(HypercubeValue)
        def val2 = Mock(HypercubeValue)
        def hypercubeValues = [val1, val2]

        dim1.name >> 'dim1'

        val1.value >> 'val 1'
        val1.getDimKey(dim1) >> 'dim1key1'
        val1.getDimKey(dim2) >> 'dim2key1'
        val2.value >> 'val 2'
        val2.getDimKey(dim1) >> 'dim1key1'
        val2.getDimKey(dim2) >> 'dim2key1'

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { hypercubeValues.iterator() }

        def view1 = new HypercubeTabularResultView(hypercube)
        def view2 = new HypercubeTabularResultView(hypercube, dimensions)
        def view3 = new HypercubeTabularResultView(hypercube, [dim1])

        when:
        view1.rows.each {}
        then:
        def e1 = thrown(AssertionError)
        e1.message.startsWith(
                'There is more then one value in the table cell with "dim1key1 / dim2key1" row and "value" column.')

        when:
        view2.rows.each {}
        then:
        def e2 = thrown(AssertionError)
        e2.message.startsWith(
                'There is more then one value in the table cell with "value" row and "dim1key1 / dim2key1" column.')

        when:
        view3.rows.each {}
        then:
        def e3 = thrown(AssertionError)
        e3.message.startsWith(
                'There is more then one value in the table cell with "dim2key1" row and "dim1key1" column.')
    }
}
