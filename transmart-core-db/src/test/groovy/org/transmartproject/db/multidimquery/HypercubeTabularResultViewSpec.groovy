package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.util.PeekingIteratorImpl
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
        val1.getDimKey(dim1) >> 'code1'
        val1.getDimKey(dim2) >> 'x1'
        val1.getDimKey(dim3) >> new Integer(1)
        val1.getDimKey(var) >> 'var1'
        val2.value >> 2.0
        val2.getDimKey(dim1) >> 'code2'
        val2.getDimKey(dim2) >> 'x2'
        val2.getDimKey(dim3) >> new Integer(2)
        val2.getDimKey(var) >> 'var1'
        val3.value >> 'val 3'
        val3.getDimKey(dim1) >> 'code2'
        val3.getDimKey(dim2) >> 'x2'
        val3.getDimKey(dim3) >> new Integer(2)
        val3.getDimKey(var) >> 'var2'
        val4.value >> 4.0
        val4.getDimKey(dim1) >> 'code3'
        val4.getDimKey(dim2) >> 'x3'
        val4.getDimKey(dim3) >> new Integer(2)
        val4.getDimKey(var) >> 'var2'

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { PeekingIteratorImpl.getPeekingIterator(hypercubeValues.iterator()) }
    }

    def 'no column dimensions'() {
        when: 'no column dimensions provided'
        List<HypercubeDataColumn> indicesList = [new HypercubeDataColumn(ImmutableMap.builder().build())]
        def view = new HypercubeTabularResultView(hypercube, dimensions, [], indicesList)
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
        rows.collect { HypercubeDataRow row -> row[indicesList[0]] } == hypercubeValues*.value
    }

    def 'no row dimensions'() {
        when: 'all dimensions provided as column dimensions'
        List<HypercubeDataColumn> indicesList = hypercubeValues.collect { HypercubeValue value ->
            def coordinates = ImmutableMap.builder()
            for (Dimension dim: dimensions) {
                coordinates.put(dim, value.getDimKey(dim))
            }
            new HypercubeDataColumn(coordinates.build())
        }
        def view = new HypercubeTabularResultView(hypercube, [], dimensions, indicesList)
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
        List<HypercubeDataColumn> indicesList = hypercubeValues.collect { HypercubeValue value ->
            def coordinates = ImmutableMap.builder()
            for (Dimension dim: columnDimensions) {
                coordinates.put(dim, value.getDimKey(dim))
            }
            new HypercubeDataColumn(coordinates.build())
        }
        def view = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions, indicesList)
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
        List<HypercubeDataColumn> indicesList = [new HypercubeDataColumn(ImmutableMap.builder().build())]
        new HypercubeTabularResultView(hypercube, [Mock(Dimension, name: 'non-existent')], [], indicesList)
        then: 'assert exception is thrown'
        def errorMessage = thrown(AssertionError)
        errorMessage.message.startsWith 'Following row dimensions are not supported by the hypercube:'
        errorMessage.message.contains('non-existent')

        when: 'not existing row dimension provided'
        new HypercubeTabularResultView(hypercube, [], [Mock(Dimension, name: 'non-existent')], indicesList)
        then: 'assert exception is thrown'
        def errorMessage2 = thrown(AssertionError)
        errorMessage2.message.startsWith 'Following column dimensions are not supported by the hypercube:'
        errorMessage2.message.contains('non-existent')
    }

    def 'hypercube is closed'() {
        when: 'view is closed'
        new HypercubeTabularResultView(hypercube, [], [], []).close()
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
        val1.getDimKey(dim1) >> 'code1'
        val2.value >> 'val 2'
        val2.getDimKey(dim1) >> 'code1'

        hypercube.dimensions >> dimensions
        hypercube.iterator() >> { PeekingIteratorImpl.getPeekingIterator(hypercubeValues.iterator()) }

        List<HypercubeDataColumn> indicesList = [new HypercubeDataColumn(ImmutableMap.builder().build())]
        def view = new HypercubeTabularResultView(hypercube, dimensions, [], indicesList)

        when:
        view.rows.each {}
        then:
        def e2 = thrown(AssertionError)
        e2.message.startsWith('There is more then one hypercube value that falls to')
    }
}
