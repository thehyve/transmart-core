package jobs.table

import com.google.common.collect.ImmutableMap
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.table.columns.PrimaryKeyColumn
import org.gmock.WithGMock
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by gustavo on 12/27/13.
 */
@TestMixin(GrailsUnitTestMixin)
@WithGMock
class TableTests {

    public static final String DATA_SOURCE_NAME = 'test_name'

    private Table testee = new Table()

    @Test
    void testSimpleCase() {
        List         sampleValues     = ['sample value 1', 'sample value 2']
        Column       column

        column = mockNonGlobalColumn(DATA_SOURCE_NAME,
                                     twoRowDataSource,
                                     twoRows,
                                     combinePKsAndValues(twoPKs, sampleValues))

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            testee.addColumn(column, [DATA_SOURCE_NAME] as Set)
            testee.buildTable()
        }

        Iterable<List<String>> result = testee.result

        assertThat result, contains(
                sampleValues.collect { is([it]) })
    }

    @Test
    void testTwoColumnsSameDataSource() {
        List         sampleValues

        sampleValues = [
                ['sample value 1-1', 'sample value 1-2'], //for column 1
                ['sample value 2-1', 'sample value 2-2']] //for column 2

        List<Column> columns = sampleValues.collect { values ->
            mockNonGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows,
                    combinePKsAndValues(twoPKs, values))
        }

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            columns.each {
                testee.addColumn(it, [DATA_SOURCE_NAME] as Set)
            }

            testee.buildTable()
        }

        assertThat testee.result, contains(
                sampleValues.transpose().collect { is it })
    }

    @Test
    void testOneGlobalColumn() {
        Map          data             = ImmutableMap.of('2', 'value 2', '1', 'value 1')
        Column       column

        column = mockGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows, data)

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            testee.addColumn(column, [DATA_SOURCE_NAME] as Set)

            testee.buildTable()
        }

        assertThat testee.result, contains(['value 1'], ['value 2'])
    }

    @Test
    void testMissingData() {
        List         sampleValues

        sampleValues = [
                ['sample value 1-1', 'sample value 1-2'], //for column 1
                ['sample value 2-1', null]] //for column 2

        List<Column> columns = sampleValues.collect { columnValues ->
            mockNonGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows,
                    combinePKsAndValues(twoPKs, columnValues))
        }


        String replacementString = 'replacement'
        columns[1].missingValueAction.returns(
                new MissingValueAction.ConstantReplacementMissingValueAction(
                        replacement: replacementString))

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            columns.each {
                testee.addColumn(it, [DATA_SOURCE_NAME] as Set)
            }

            testee.buildTable()

            assertThat testee.result, contains(
                    sampleValues.transpose().collect { is it.collect { it ?: replacementString } })
        }
    }

    @Test
    void testUnsubscribedNonGlobalColumn() {
        testUnsubscribedColumn(false)
    }

    /* unsubscribed global calculation columns are a strange concept, but let's
     * allow this */
    @Test
    void testUnsubscribedGlobalColumn() {
        testUnsubscribedColumn(true)
    }

    private void testUnsubscribedColumn(boolean globalComputation) {
        Iterable sampleDataSource = mockDataSource([mock(Object)])
        Column column  = mock(Column)

        column.globalComputation.returns(globalComputation).atLeast(1)

        column.consumeResultingTableRows().returns(ImmutableMap.of('foo', 'bar'))

        column.onAllDataSourcesDepleted(0, isA(BackingMap))

        play {
            testee.addDataSource DATA_SOURCE_NAME, sampleDataSource
            testee.addColumn(column, [] as Set)

            testee.buildTable()
        }

        assertThat testee.result, contains(is(['bar']))
    }

    @Test
    void testMixedGlobalNonGlobalColumns() {
        List<Column> columns          = []
        List         sampleValues1
        Map          sampleValues2

        sampleValues1 = ['sample non global 1', 'sample non global 2']
        sampleValues2 = ImmutableMap.of(twoPKs[1], 'global')

        columns << mockNonGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows,
                [ ImmutableMap.of(twoPKs[0], sampleValues1[0]),
                        ImmutableMap.of(twoPKs[1], sampleValues1[1]) ])
        columns << mockGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource,
                twoRows, sampleValues2)

        columns.last().missingValueAction.returns(
                new MissingValueAction.ConstantReplacementMissingValueAction(
                        replacement: 'foo'))

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            columns.each { testee.addColumn(it, [DATA_SOURCE_NAME] as Set) }

            testee.buildTable()

            assertThat testee.result, contains(
                    is(['sample non global 1', 'foo']),
                    is(['sample non global 2', 'global']),
            )
        }

    }

    @Test
    void testTwoDataSourcesOnePerColumn() {
        List<List<Object>> rows = [ [mock(Object)], [mock(Object), mock(Object)] ]
        List dataSourceNames = [ DATA_SOURCE_NAME, DATA_SOURCE_NAME + '2' ]
        Iterable dataSources = rows.collect { mockDataSource it }
        List rowPKs = ['rowPK1']
        List<Column> columns = []

        columns << mockGlobalColumn(dataSourceNames[0], dataSources[0], rows[0],
                ImmutableMap.of(rowPKs[0], 'datasource 1'))
        columns << mockNonGlobalColumn(dataSourceNames[1], dataSources[1], rows[1],
                [ ImmutableMap.of(rowPKs[0], 'datasource 2'), ImmutableMap.of() ])

        play {
            (0..1).each {
                testee.addDataSource(dataSourceNames[it], dataSources[it])
                testee.addColumn(columns[it], [dataSourceNames[it]] as Set)
            }

            testee.buildTable()
        }

        assertThat testee.result, contains(
                is(['datasource 1', 'datasource 2']))
    }

    @Test
    void testTwoDataSourcesOnOneColumn() {
        List<List<Object>> rows = [
                [ mock(Object), mock(Object) ],
                [ mock(Object) ] ]
        List dataSourceNames = [ DATA_SOURCE_NAME, DATA_SOURCE_NAME + '2' ]
        Iterable dataSources = rows.collect { mockDataSource it }

        Column column  = mock(Column)
        column.globalComputation.returns(false).atLeast(1)

        ordered {
            /* Overspecified. Implementations should not rely on interleaving */
            column.beforeDataSourceIteration(dataSourceNames[0], dataSources[0])
            column.beforeDataSourceIteration(dataSourceNames[1], dataSources[1])

            /* order in which the data sources are read is unknown */
            unordered {
                column.onReadRow dataSourceNames[0], rows[0][0]
                column.onReadRow dataSourceNames[1], rows[1][0]
                //underspecified here, this cannot be the first call on this block
                column.consumeResultingTableRows().returns(ImmutableMap.of('1', '1')).atLeast(1)
            }

            column.onDataSourceDepleted dataSourceNames[1], dataSources[1]
            column.onReadRow dataSourceNames[0], rows[0][1]
            column.consumeResultingTableRows().returns(ImmutableMap.of('2', '2'))

            column.onDataSourceDepleted dataSourceNames[0], dataSources[0]
            column.onAllDataSourcesDepleted(0, isA(BackingMap))
        }

        play {
            (0..1).each {
                testee.addDataSource(dataSourceNames[it], dataSources[it])
            }
            testee.addColumn(column, dataSourceNames as Set)

            testee.buildTable()
        }

        assertThat testee.result, contains(is(['1']), is(['2']))
    }

    @Test
    void testNullReplacementsRemoveLine() {
        List<Column> columns
        List         data

        data = [
                [ 'col 1, row 1', 'col 1, row 2' ],
                [ null,           'col 2, row 2' ],
        ]

        columns = data.collect { values ->
            mockNonGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows,
                    combinePKsAndValues(twoPKs, values))
        }

        columns.last().missingValueAction.returns(
                new MissingValueAction.DropRowMissingValueAction())

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            columns.each { testee.addColumn(it, [DATA_SOURCE_NAME] as Set) }

            testee.buildTable()

            assertThat testee.result, contains(
                    is(['col 1, row 2', 'col 2, row 2']))
        }
    }

    @Test
    void testOnAllDataSourcesDepleted() {
        List<Column> columns = []
        List         values

        values = [ 'row1', 'row2' ]

        columns << new PrimaryKeyColumn()
        columns << mockNonGlobalColumn(DATA_SOURCE_NAME, twoRowDataSource, twoRows,
                combinePKsAndValues(twoPKs, values))

        play {
            testee.addDataSource DATA_SOURCE_NAME, twoRowDataSource
            columns.each { testee.addColumn(it, [DATA_SOURCE_NAME] as Set) }

            testee.buildTable()

            assertThat testee.result, contains(
                    is([twoPKs[0], values[0]]),
                    is([twoPKs[1], values[1]]),)
        }
    }

    private @Lazy List<Object> twoRows = [ mock(Object), mock(Object) ]

    private @Lazy List<String> twoPKs = ['rowPK1', 'rowPK2']

    private @Lazy Iterable twoRowDataSource = mockDataSource(twoRows)

    private Column mockNonGlobalColumn(String dataSourceName,
                                       Iterable dataSource,
                                       List<Object> rows,
                                       List<Map> returnValues) {
        if (returnValues.size() != rows.size()) {
            throw new RuntimeException('Expected returnValues.size() == rows.size()')
        }

        Column column  = mock(Column)

        column.globalComputation.returns(false).atLeast(1)

        ordered {
            column.beforeDataSourceIteration(dataSourceName, dataSource)

            for (int i = 0; i < rows.size(); i++) {
                column.onReadRow(dataSourceName, rows[i])

                column.consumeResultingTableRows().returns(returnValues[i])
            }

            // this is overspecified here. onDataSourceDepleted() could be called
            // before the last consumeResultingTableRows(), or
            // consumeResultingTableRows() could be called again afterwards.
            column.onDataSourceDepleted(dataSourceName, dataSource)

            column.onAllDataSourcesDepleted(isA(Integer), isA(BackingMap))
        }

        column
    }

    private Column mockGlobalColumn(String dataSourceName,
                                    Iterable dataSource,
                                    List<Object> rows,
                                    Map returnValue) {
        Column column = mock(Column)

        column.globalComputation.returns(true).atLeast(1)

        ordered {
            column.beforeDataSourceIteration(dataSourceName, dataSource)

            for (r in rows) {
                column.onReadRow dataSourceName, r
            }

            //depletion comes before here!
            column.onDataSourceDepleted(dataSourceName, dataSource)

            column.consumeResultingTableRows().returns(returnValue)

            column.onAllDataSourcesDepleted(isA(Integer), isA(BackingMap))
        }

        column
    }

    private Iterable mockDataSource(List rows) {
        Iterable result = mock(Iterable)

        ordered {
            Iterator iterator = mock(Iterator)

            result.iterator().returns(iterator)

            for (r in rows) {
                iterator.hasNext().returns(true).atLeast(1)
                iterator.next().returns(r)
            }

            iterator.hasNext().returns(false).atLeast(1)
        }

        result
    }

    private combinePKsAndValues(List rowPKs, List columnValues) {
        (0..(rowPKs.size()-1)).collect {
            if (columnValues[it] == null) {
                ImmutableMap.of()
            } else {
                ImmutableMap.of(rowPKs[it], columnValues[it])
            }
        }
    }
}
