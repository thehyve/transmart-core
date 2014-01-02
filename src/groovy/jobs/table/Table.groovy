package jobs.table

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.*
import groovy.transform.CompileStatic
import org.mapdb.Fun

@CompileStatic
class Table {

    private BiMap<String, Iterable> dataSources = HashBiMap.create()

    private List<Column> columns = Lists.newArrayList()

    private SortedSetMultimap<Iterable, Column> dataSourceSubscriptions

    private BackingMap backingMap

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
        dataSources[name] = dataSource
    }

    void addColumn(Column col, Set<String> subscribedDataSources) {
        def difference = subscribedDataSources - dataSources.keySet()
        if (difference) {
            throw new IllegalStateException("Cannot subscribe unknown " +
                    "data sources: $difference")
        }

        columns << col

        for (dataSourceName in subscribedDataSources) {
            dataSourceSubscriptions.put(dataSources[dataSourceName], col)
        }
    }

    void buildTable() {
        backingMap = new BackingMap(columns.size())

        boolean simpleCase = dataSources.size() == 1 &&
                !columns.any { Column it ->
                    it.globalComputation
                } //tentative

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

    Iterable<List<String>> getResult() {
        if (backingMap == null) {
            throw new IllegalStateException('buildTable() has not been called')
        }

        def transformed = Iterables.transform(backingMap.rowIterable,
                { Fun.Tuple2<String, List<String>> from ->
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

        Iterables.filter(transformed, { it != null } as Predicate)
    }

    /**
     * Build table in the simple case where there's only one data source and
     * none of the columns are global computation columns.
     */
    private void buildTableSimpleCase() {
        String dataSourceName = Iterables.getFirst dataSources.keySet(), null
        Iterable dataSource = (Iterable) Iterables.getFirst(dataSources.values(), null)

        dataSource.each { Object row ->
            processSourceRow row, dataSourceName, dataSource
        }

        afterIterableDepleted dataSourceName, dataSource
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

        /* consume rows for GlobalComputationColumn columns
         * and columns without subscriptions */
        columns.findAll { Column it ->
            it.globalComputation ||
                    !dataSourceSubscriptions.values().contains(it)
        }.collectEntries { Column it -> [columns.indexOf(it),
                it.consumeResultingTableRows() ]
        }.each { int columnNumber, Map<String, String> values ->
            values.each { String primaryKey, String cellValue ->
                backingMap.putCell primaryKey, columnNumber, cellValue
            }
        }
    }

    private void processSourceRow(Object row, String dataSourceName, Iterable dataSource) {
        dataSourceSubscriptions.get(dataSource).eachWithIndex{ Column col,
                                                               Integer colNumber ->
            col.onReadRow dataSourceName, row
            if (col.globalComputation) {
                return
            }
            col.consumeResultingTableRows().each { String pk,
                                                   String value ->
                backingMap.putCell pk, colNumber, value
            }
        }
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
            col.onDataSourceDepleted dataSourceName, dataSource, backingMap
        }
    }


}
