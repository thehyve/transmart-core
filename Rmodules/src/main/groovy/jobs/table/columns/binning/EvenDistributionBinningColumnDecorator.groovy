package jobs.table.columns.binning

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovyx.gpars.GParsPool
import jobs.table.BackingMap
import jobs.table.Column
import jobs.table.columns.ColumnDecorator
import org.mapdb.Fun

/**
 * Quantile calculation and rewriting of the results.
 *
 * Memory requirements could be lowered by having by using an approximation.
 */
@CompileStatic
class EvenDistributionBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    /* number of subsets */
    int numberOfBins

    /* context -> (upper bound -> bin name) */
    private Map<String, NavigableMap<Number, String>> bins =
            Maps.newConcurrentMap()

    static class GroovyNumberComparator implements Comparator<Number> {
        public static GroovyNumberComparator INSTANCE =
                new GroovyNumberComparator()

        int compare(Number o1, Number o2) {
            o1 <=> o2
        }
    }

    private NavigableMap<Number, String> generateBinNames(List<Number> sortedValues,
                                                          List<Integer> quantileRanks) {
        NavigableMap<Number, String> res =
                Maps.newTreeMap(GroovyNumberComparator.INSTANCE)

        int effectiveNumberOfBins = quantileRanks.size()

        (0..(effectiveNumberOfBins - 1)).each { Integer it ->
            Number lowerBound = it == 0 ?
                    sortedValues[0] :
                    sortedValues[quantileRanks[it - 1]]
            Number upperBound = it == (effectiveNumberOfBins - 1) ?
                    sortedValues[-1] :
                    sortedValues[quantileRanks[it]]

            def op1 = it == 0 ? '≤' : '<'
            res[upperBound] = "$lowerBound $op1 $header ≤ $upperBound" as String
        }

        res
    }

    private List<Number> collectAndSortValues(BackingMap backingMap,
                                              Collection<String> pks,
                                              Integer columnNumber,
                                              String context) {
        List<Number> res = Lists.newArrayListWithCapacity pks.size()
        pks.each { String primaryKey ->
            Number n = (Number) backingMap.getCell(primaryKey, columnNumber, context)
            res.add n
        }

        Collections.sort res, GroovyNumberComparator.INSTANCE

        res
    }

    private List<Integer> calculateQuantileRanks(List<Number> sortedValues) {
        List<Integer> res = Lists.newArrayListWithCapacity numberOfBins

        int effectiveNumberOfBins = Math.min numberOfBins, sortedValues.size()

        for (i in 1..effectiveNumberOfBins) {
            /* maybe use nearest rank instead? */
            int rank = (((sortedValues.size() * i) / effectiveNumberOfBins) as int) - 1 //rounds down
            while (rank > 0 &&
                    sortedValues[rank] == sortedValues[rank - 1]) {
                rank--
            }
            // rank will be first element with this value
            // if this results in the rank being the same as the rank for the
            // last bin, then this bin will essentially be filled.
            // we may be able to do better than this*, but ultimately there's no
            // way to evenly distribute a lot of element with the same value
            // unless we start putting equal elements in different bins, which
            // would be confusing given the labels of the bins

            // * for instance, given 2 bins, we could do
            // 20 20 20 20 | 30 | 31
            // but given the way we do it now is:
            // 20 20 20 20 | 30 31
            if (i > 1 && res.last() == rank) {
                continue
            }
            res << rank
        }

        res
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    void onAllDataSourcesDepleted(int columnNumber,
                                  BackingMap backingMap) {
        Map<String, Set<String>> contextPkMap =
                backingMap.getContextPrimaryKeysMap(columnNumber)

        // XXX: create a reusable pool for this sort of thing
        GParsPool.withPool {
            contextPkMap.eachParallel { String ctx, Set<String> pks ->
                List<Number> sortedValues =
                        collectAndSortValues backingMap, pks, columnNumber, ctx
                List<Integer> quantileRanks =
                        calculateQuantileRanks sortedValues

                bins[ctx] = generateBinNames sortedValues, quantileRanks
            }
        }
    }

    // NOTE: assumes there's no transformer in inner
    Closure<Object> getValueTransformer() {
        { Fun.Tuple3<String, Integer, String> key, Object value ->
            Number numberValue = (Number) value

            String ctx = key.c
            NavigableMap<Number, String> binsForCtx = bins[ctx]

            Map.Entry<Number, String> entry = binsForCtx.ceilingEntry(numberValue)
            if (entry == null) {
                // should not happen
                throw new IllegalStateException("Could not determine bin for " +
                        "value $numberValue. Bins available, with key as upper " +
                        "bound: $binsForCtx")
            }

            (Object) entry.value /* the name of the bin */
        }
    }

    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        // just for validation
        if (!header) {
            throw new IllegalStateException('Bug: header not set here')
        }

        inner.beforeDataSourceIteration dataSourceName, dataSource
    }
}

