package jobs.table

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.*
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.mapdb.Fun
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class Table implements AutoCloseable {

    private BiMap<String, Iterable> dataSources = HashBiMap.create()

    private List<Column> columns = Lists.newArrayList()

    private Map<Column, Integer> columnsToIndex = Maps.newHashMap()

    private SortedSetMultimap<Iterable, Column> dataSourceSubscriptions

    private BackingMap backingMap

    private int droppedRows = 0

    int getDroppedRows() {
        droppedRows
    }

    {
        //before adding a subscription, column must be in list
        dataSourceSubscriptions = TreeMultimap.create(
                { a, b ->
                    dataSources.inverse().get(a) <=>
                            dataSources.inverse().get(b)
                } as Comparator,
                { a, b ->
                    columns.indexOf(a) <=> columns.indexOf(b)
                } as Comparator)

    }

    void addDataSource(String name, Iterable dataSource) {
        if (dataSources.containsKey(name) && dataSources[name] != dataSource) {
            throw new IllegalStateException("Data source with $name " +
                    "already exists and it is different. Currently " +
                    "registered data source under this name is $dataSource")
        }
        if (dataSources.containsValue(dataSource) &&
                dataSources.inverse().get(dataSource) != name) {
            throw new IllegalStateException("Data source $dataSource " +
                    "was already registered under the name " +
                    "${dataSources.inverse().get(dataSource)}; " +
                    "given name $name this time")
        }

        dataSources[name] = dataSource
    }

    String getDataSourceName(Iterable dataSource) {
        dataSources.inverse()[dataSource]
    }

    Iterable getDataSource(String name) {
        dataSources[name]
    }

    void addColumn(Column col, Set<String> subscribedDataSources) {
        def difference = subscribedDataSources - dataSources.keySet()
        if (difference) {
            throw new IllegalStateException("Cannot subscribe unknown " +
                    "data sources: $difference")
        }

        validateColumn col

        columns << col
        columnsToIndex[col] = columns.size() - 1

        for (dataSourceName in subscribedDataSources) {
            dataSourceSubscriptions.put(dataSources[dataSourceName], col)
        }
    }

    private validateColumn(Column column) {
        if (!column.header) {
            throw new IllegalStateException("Header not set on column $column")
        }
        if (!column.missingValueAction) {
            throw new IllegalStateException(
                    "Missing value action not set on column $column")
        }
    }

    void buildTable() {
        backingMap = new BackingMap(columns.size(),
                columns.collect { Column it -> it.valueTransformer })

        boolean simpleCase = dataSources.size() == 1

        if (simpleCase) {
            Iterable dataSource =
                    (Iterable) Iterables.getFirst(dataSources.values(), null)
            if (dataSourceSubscriptions.get(dataSource) as Set !=
                    columns as Set) {
                simpleCase = false // at least one column doesn't subscribe
            }
        }

        beforeIteration()

        if (simpleCase) {
            buildTableSimpleCase()
        } else {
            buildTableGeneralCase()
        }
    }

    Iterable<List<Object>> getResult() {
        if (backingMap == null) {
            throw new IllegalStateException('buildTable() has not been called')
        }

        def transformed = Iterables.transform(backingMap.rowIterable,
                { Fun.Tuple2<String, List<Object>> from -> /* (pk, list of values) */
                    for (int i = 0; i < from.b.size(); i++) {
                        if (from.b[i] == null) {
                            def replacement =
                                    columns[i].missingValueAction.getReplacement(from.a)
                            if (replacement == null) {
                                return null // row will be dropped next
                            }
                            from.b[i] = replacement
                        }
                    }

                    from.b //only the list matters to the outside
                } as Function)

        Iterables.filter(transformed, {
            if (it != null) {
                true
            } else {
                droppedRows++
                false
            }
        } as Predicate)
    }

    /**
     * Build table in the simple case where there's only one data source.
     */
    private void buildTableSimpleCase() {
        String dataSourceName = Iterables.getFirst dataSources.keySet(), null
        Iterable dataSource = (Iterable) Iterables.getFirst(dataSources.values(), null)

        for (Object row in dataSource) {
            processSourceRow row, dataSourceName, dataSource
        }

        afterIterableDepleted dataSourceName, dataSource
        afterAllDataSourcesDepleted()
    }

    private void buildTableGeneralCase() {
        Map<String, Iterator> liveDataSources =
                (Map) dataSources.collectEntries { String dataSourceName,
                                                   Iterable dataSource ->
                 [ dataSourceName, dataSource.iterator() ]
            }

        while (!liveDataSources.isEmpty()) {
            Set dataSourcesToRemove = Sets.newHashSet()
            liveDataSources.each { String dataSourceName,
                                   Iterator dataSourceIterator ->

                if (!dataSourceIterator.hasNext()) {
                    dataSourcesToRemove << dataSourceName
                    afterIterableDepleted dataSourceName, dataSources[dataSourceName]
                    return
                }

                Object dataSourceRow = dataSourceIterator.next()

                processSourceRow dataSourceRow, dataSourceName, dataSources[dataSourceName]
            }

            // can't be done inside liveDataSources.each(): ConcurrentModificationException
            dataSourcesToRemove.each { liveDataSources.remove it }
            dataSourcesToRemove.clear()
        }

        quasiFinalProcessing()
        afterAllDataSourcesDepleted()
    }

    private Map quasiFinalProcessing() {
        /* consume rows for columns without subscriptions */

        columns.findAll { Column it ->
            !dataSourceSubscriptions.values().contains(it)
        }.collectEntries { Column it ->
            [columns.indexOf(it),
                    it.consumeResultingTableRows()]
        }.each { int columnNumber, Map<String, Object> values ->
            if (values == null) {
                throw new NullPointerException("Column $columnNumber returned a null map")
            }
            values.each { String primaryKey, Object cellValue ->
                putCellToBackingMap primaryKey, columnNumber, cellValue
            }
        }
    }

    private void processSourceRow(Object row, String dataSourceName, Iterable dataSource) {
        dataSourceSubscriptions.get(dataSource).each { Column col ->
            col.onReadRow dataSourceName, row
            def consumedRows = col.consumeResultingTableRows()
            consumedRows.each { String pk, Object value ->
                putCellToBackingMap pk, columnsToIndex[col], value
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    // disable @CompileStatic to have multi-dispatch
    private void putCellToBackingMap(String pk, Integer column, Object value) {
        backingMap.putCell pk, column, value
    }

    private void beforeIteration() {
        dataSourceSubscriptions.asMap().each { Iterable dataSource,
                                               Collection<Column> columns ->
            columns.each { Column col ->
                col.beforeDataSourceIteration(
                        dataSources.inverse().get(dataSource),
                        dataSource)
            }
        }
    }

    private void afterIterableDepleted(String dataSourceName,
                                       Iterable dataSource) {
        dataSourceSubscriptions.get(dataSource).each { Column col ->
            col.onDataSourceDepleted dataSourceName, dataSource
        }
    }

    private void afterAllDataSourcesDepleted() {
        columns.eachWithIndex { Column column, int index ->
            column.onAllDataSourcesDepleted(index, backingMap)
        }
    }

    List<String> getHeaders() {
        columns.collect { Column it ->
            it.header
        }
    }

    @Override
    void close() {
        def backingMapException

        try {
            backingMap?.close()
            backingMap = null
        } catch (Exception e) {
            backingMapException = e
        }

        def dataSourceExceptions = [:]

        for (Iterable dataSource in dataSources.values()) {
            try {
                if (dataSource instanceof Closeable) {
                    ((Closeable) dataSource).close()
                } else if (dataSource instanceof AutoCloseable) {
                    ((AutoCloseable) dataSource).close()
                }
            } catch (Exception e) {
                dataSourceExceptions[dataSource] = e
            }
        }

        if (backingMapException != null) {
            throw backingMapException
        }

        if (!dataSourceExceptions.isEmpty()) {
            throw new RuntimeException("One or more dataSourceExceptions when " +
                    "closing data sources: " + dataSourceExceptions.collect {
                Iterable ds, Exception e ->
                "$ds (${dataSources.inverse().getAt(ds)}): $e.message"
            }.join(', '), (Throwable)Iterables.getFirst(dataSourceExceptions.values(), null))
        }
    }
}
