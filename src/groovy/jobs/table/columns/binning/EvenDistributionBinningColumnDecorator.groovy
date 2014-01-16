package jobs.table.columns.binning

import com.google.common.base.Function
import com.google.common.collect.*
import jobs.table.Column
import jobs.table.columns.ColumnDecorator

/**
 * Quantile calculation and rewriting of the results.
 *
 * Because we only have one pass of the data and we're doing an exact
 * computation, this will require O(N) memory.
 *
 * Memory requirements could be lowered by having more passes or using
 * approximation.
 */
class EvenDistributionBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    int numSubscribedDataSources = 1

    /* number of subsets */
    int numberOfBins

    boolean isGlobalComputation() {
        true
    }

    private Map<String, List<Map.Entry>> allResults = {
        def res = [:]
        res.withDefault { ctx ->
            res[ctx] = Lists.newArrayList()
        }
    }()

    private Map<String, List> quantileRanks = Maps.newHashMap()

    private Map<String, List> binNames = {
        def res = [:]
        res.withDefault { ctx ->
            res[ctx] = (0..(numberOfBins - 1)).collect {
                def lowerBound = it == 0 ? allResults[ctx][0].value :
                                           allResults[ctx][quantileRanks[ctx][it - 1]].value
                def upperBound = it == (numberOfBins - 1) ? allResults[ctx][-1].value :
                                                            allResults[ctx][quantileRanks[ctx][it]].value
                def op1 = it == 0 ? '≤' : '<'
                "$lowerBound $op1 $header ≤ $upperBound" as String
            }
        }
    }()

    void onReadRow(String dataSourceName, Object row) {
        inner.onReadRow(dataSourceName, row)
        if (!inner.globalComputation) {
            consumeResultingTableRows()
        }
    }

    private void consumeEntry(String pk, String ctx, Number value) {
        allResults[ctx] << Maps.immutableEntry(pk, value)
    }

    private void consumeEntry(String pk, String ctx, Object value) {
        consumeEntry(pk, ctx, value as BigDecimal)
    }

    private void consumeEntry(String pk, String ctx, Map value) {
        /* otherwise found map inside map inside consumeResultingTableRows()'s map? */
        assert ctx == ''
        for (entry in value) {
            consumeEntry pk, entry.key, entry.value
        }
    }

    Map<String, Object> consumeResultingTableRows() {
        if (allResults == null) {
            /* already gave back its data */
            return ImmutableMap.of()
        }

        for (entry in inner.consumeResultingTableRows()) {
            consumeEntry entry.key, '', entry.value
        }

        if (numSubscribedDataSources > 0) {
            return ImmutableMap.of()
        }

        calculateQuantiles()

        /* use of ImmutableTable is not possible yet (not serializable) */
        /* pk -> context, value */
        Map<String, ImmutableMap.Builder<String, Object>> builders = [:]
        builders = builders.withDefault { key ->
            builders[key] = ImmutableMap.builder()
        }

        def contextSet = allResults.keySet()
        contextSet.each { ctx ->
            transformContext ctx, builders
        }
        allResults = null

        def provisionalResult = Maps.transformValues(builders,
                { ImmutableMap.Builder builder ->
                    builder.build()
                } as Function)

        if (contextSet == [''] as Set) {
            Maps.transformValues(provisionalResult, {
                it['']
            } as Function)
        } else {
            provisionalResult
        }
    }

    void transformContext(String ctx,
                          Map<String, ImmutableMap.Builder<String, Object>> builders) {
        List<Map.Entry> ctxResults = allResults[ctx]
        List ranks = quantileRanks[ctx]


        def k = 1 /* number of quantile */
        def nextTransition = ranks[k - 1]

        for (i in 0..(ctxResults.size() - 1)) {
            while (i > nextTransition) {
                k++
                nextTransition = ranks[k - 1]
            }
            builders[ctxResults[i].key].put ctx, binNames[ctx][k - 1]
        }
    }

    void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
        --numSubscribedDataSources
        inner.onDataSourceDepleted(dataSourceName, dataSource)
    }

    void calculateQuantiles() {
        allResults.values().each { List<Map.Entry> entryList ->
            Collections.sort(entryList, { Map.Entry a, Map.Entry b ->
                a.value <=> b.value
            } as Comparator)
        }

        for (ctxEntry in allResults) {
            String ctx = ctxEntry.key
            List<Map.Entry> ctxResults = ctxEntry.value

            quantileRanks[ctx] = []
            for (i in 1..numberOfBins) {
                /* maybe use nearest rank instead? */
                def rank = (((ctxResults.size() * i) / numberOfBins) as int) - 1 //rounds down
                while (rank < ctxResults.size() - 2 &&
                        ctxResults[rank].value == ctxResults[rank + 1].value) {
                    rank++
                }
                quantileRanks[ctx] << rank
            }
            quantileRanks[ctx] << ctxResults.size() // to ease impl of consumeResultingTableRows()
        }
    }
}
