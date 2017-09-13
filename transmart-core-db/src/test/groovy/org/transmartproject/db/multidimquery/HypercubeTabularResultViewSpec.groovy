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
        val1.getDimElementIndex(dim1) >> 1
        val1.getDimElementIndex(dim2) >> 1
        val1.getDimElementIndex(dim3) >> 1
        val1.getDimElementIndex(var) >> 1
        val2.value >> 2.0
        val2.getDimElementIndex(dim1) >> 2
        val2.getDimElementIndex(dim2) >> 2
        val2.getDimElementIndex(dim3) >> 2
        val2.getDimElementIndex(var) >> 1
        val3.value >> 'val 3'
        val3.getDimElementIndex(dim1) >> 2
        val3.getDimElementIndex(dim2) >> 2
        val3.getDimElementIndex(dim3) >> 2
        val3.getDimElementIndex(var) >> 2
        val4.value >> 4.0
        val4.getDimElementIndex(dim1) >> 3
        val4.getDimElementIndex(dim2) >> 3
        val4.getDimElementIndex(dim3) >> 2
        val4.getDimElementIndex(var) >> 2

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { hypercubeValues.iterator() }
    }

    def 'no column dimensions'() {
        when: 'no column dimensions provided'
        def view = new HypercubeTabularResultView(hypercube, dimensions)
        then: 'all dimensions are row dimensions'
        view.columnsDimensionLabel == HypercubeTabularResultView.NO_DIMENSIONS_LABEL
        view.columnDimensions.size() == 0
        view.rowsDimensionLabel == dimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.rowDimensions == dimensions

        when: 'columns are read'
        def columns = view.indicesList
        then: 'there is only one value column'
        columns.size() == 1

        when: 'rows are read'
        def rows = view.rows.toList()
        then: 'rows content matches hypercube values respecting order'
        rows.collect { row -> row[columns[0]] } == hypercubeValues*.value
    }

    def 'no row dimensions'() {
        when: 'all dimensions provided as column dimensions'
        def view = new HypercubeTabularResultView(hypercube, [], dimensions)
        then: 'there are no row dimensions'
        view.columnsDimensionLabel == dimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.columnDimensions == dimensions
        view.rowsDimensionLabel == HypercubeTabularResultView.NO_DIMENSIONS_LABEL
        view.rowDimensions.size() == 0

        when: 'columns are read'
        def columns = view.indicesList
        then: 'there are as many columns as values'
        columns.size() == hypercubeValues.size()

        when: 'rows are read'
        def rows = view.rows.toList()
        then: 'row labels are values'
        rows.size() == 1
        then: 'the row values match in regards to the indices order'
        columns.collect { column -> rows[0][column] } == hypercubeValues*.value
    }

    def 'row and column dimensions'() {
        def rowDimensions = [dim1, dim3]
        def columnDimensions = [var, dim2]

        when: 'column dimensions provided'
        def view = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions)
        then: 'column dimensions and row dimensions slitted with respect to the their order'
        view.columnDimensions == columnDimensions
        view.columnsDimensionLabel == columnDimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.rowDimensions == rowDimensions
        view.rowsDimensionLabel == rowDimensions*.name.join(HypercubeTabularResultView.DIMENSION_ELEMENTS_DELIMITER)
        view.indicesList.size() == 4
        view.rows.size() == 3
    }

    def 'not existing column dimension'() {
        when: 'not existing row dimension provided'
        new HypercubeTabularResultView(hypercube, [Mock(Dimension, name: 'non-existent')])
        then: 'assert exception is thrown'
        def errorMessage = thrown(AssertionError)
        errorMessage.message.startsWith 'Following row dimensions are not supported by the hypercube:'
        errorMessage.message.contains('non-existent')

        when: 'not existing row dimension provided'
        new HypercubeTabularResultView(hypercube, [], [Mock(Dimension, name: 'non-existent')])
        then: 'assert exception is thrown'
        def errorMessage2 = thrown(AssertionError)
        errorMessage2.message.startsWith 'Following column dimensions are not supported by the hypercube:'
        errorMessage2.message.contains('non-existent')
    }

    def 'hypercube is closed'() {
        when: 'view is closed'
        new HypercubeTabularResultView(hypercube).close()
        then: 'hypercube is closed'
        with(hypercube) {
            1 * close()
        }
    }

    def 'values collision'() {
        def hypercube = Mock(Hypercube)
        def dim1 = Mock(Dimension)
        def dimensions = [dim1]
        def val1 = Mock(HypercubeValue)
        def val2 = Mock(HypercubeValue)
        def hypercubeValues = [val1, val2]

        dim1.name >> 'dim1'

        val1.value >> 'val 1'
        val1.getDimElementIndex(dim1) >> 1
        val2.value >> 'val 2'
        val2.getDimElementIndex(dim1) >> 1

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { hypercubeValues.iterator() }

        def view = new HypercubeTabularResultView(hypercube, dimensions)

        when:
        view.rows.each {}
        then:
        def e2 = thrown(AssertionError)
        e2.message.startsWith('There is more then one hypercube value that falls to')
    }
}
