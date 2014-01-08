package jobs.table.columns.binning

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
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

    private List<Map.Entry> allResults = Lists.newArrayList()

    private List quantileRanks

    private @Lazy List binNames = {
        (0..(numberOfBins - 1)).collect {
            def lowerBound = it == 0 ? allResults[0].value :
                                       allResults[quantileRanks[it - 1]].value
            def upperBound = it == (numberOfBins - 1) ? allResults[-1].value :
                                                        allResults[quantileRanks[it]].value
            def op1 = it == 0 ? '≤' : '<'
            "$lowerBound $op1 $header ≤ $upperBound" as String
        }
    }()

    void onReadRow(String dataSourceName, Object row) {
        inner.onReadRow(dataSourceName, row)
        if (!inner.globalComputation) {
            consumeResultingTableRows()
        }
    }

    Map<String, String> consumeResultingTableRows() {
        if (allResults == null) {
            /* already gave back its data */
            return ImmutableMap.of()
        }

        for (entry in inner.consumeResultingTableRows()) {
            allResults << entry
        }

        if (numSubscribedDataSources > 0) {
            return ImmutableMap.of()
        }

        calculateQuantiles()
        def res = ImmutableMap.builder()

        def k = 1 /* number of quantile */
        def nextTransition = quantileRanks[k - 1]

        for (i in 0..(allResults.size() - 1)) {
            while (i > nextTransition) {
                k++
                nextTransition = quantileRanks[k - 1]
            }
            res.put(allResults[i].key, binNames[k - 1])
        }

        allResults = null
        res.build()
    }

    void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
        --numSubscribedDataSources
    }

    void calculateQuantiles() {
        Collections.sort(allResults, { Map.Entry a, Map.Entry b ->
            (a.value as BigDecimal) <=> (b.value as BigDecimal)
        } as Comparator)

        quantileRanks = []
        for (i in 1..numberOfBins) {
            /* maybe use nearest rank instead? */
            def rank = (((allResults.size() * i) / numberOfBins) as int) - 1 //rounds down
            while (rank < allResults.size() - 2 &&
                    (allResults[rank].value as BigDecimal) == (allResults[rank + 1].value as BigDecimal)) {
                rank++
            }
            quantileRanks << rank
        }
        quantileRanks << allResults.size() // to ease impl of consumeResultingTableRows()
    }
}
