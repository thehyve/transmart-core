package jobs.table.columns.binning

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import jobs.table.Column
import jobs.table.columns.ColumnDecorator

class EvenSpacedBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    int numberOfBins

    int numSubscribedDataSources = 1

    boolean isGlobalComputation() { true }

    private List<Map.Entry> allResults = Lists.newArrayList()

    private Object min = Double.POSITIVE_INFINITY,
                   max = Double.NEGATIVE_INFINITY

    private @Lazy List binNames = {
        (1..numberOfBins).collect {
            def lowerBound = min + ((max - min) / numberOfBins) * (it - 1)
            def upperBound = min + ((max - min) / numberOfBins) * it
            def op2 = it == numberOfBins ? '≤' : '<'
            "$lowerBound ≤ $header $op2 $upperBound" as String
        }
    }()


    Map<String, String> consumeResultingTableRows() {
        if (allResults == null) {
            return ImmutableMap.of()
        }

        for (entry in inner.consumeResultingTableRows()) {
            def valueAsBigDecimal = entry.value as BigDecimal
            if (valueAsBigDecimal < min) {
                min = valueAsBigDecimal
            }
            if (valueAsBigDecimal > max) {
                max = valueAsBigDecimal
            }
            //store as bigdecimal because we'll need another comparison later on
            allResults << Maps.immutableEntry(entry.key, valueAsBigDecimal)
        }

        if (numSubscribedDataSources > 0) {
            return ImmutableMap.of()
        }

        def res = ImmutableMap.builder()

        def c = numberOfBins / (max - min)
        for (entry in allResults) {
            /* normalize to interval [0, numberOfBins] */
            def norm = (entry.value - min) * c
            def bin = (norm as int)
            assert bin >= 0
            if (bin == numberOfBins) { //happens for max
                bin--
            }
            res.put(entry.key, binNames[bin])
        }

        allResults = null
        res.build()
    }

    void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
        --numSubscribedDataSources
    }

}
