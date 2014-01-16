package jobs.table.columns.binning

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Table
import com.google.common.collect.TreeBasedTable
import jobs.table.Column
import jobs.table.columns.ColumnDecorator

class EvenSpacedBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    int numberOfBins

    int numSubscribedDataSources = 1

    boolean isGlobalComputation() { true }

    /* pk, context (e.g. probe name), value */
    private Table<String, String, Number> allResults = TreeBasedTable.create()

    private Map<String, Number> min = [:].withDefault { Double.POSITIVE_INFINITY },
                                max = [:].withDefault { Double.NEGATIVE_INFINITY }

    private Map<String, List> binNames = {
        def ret = [:]
        ret.withDefault { ctx ->
            ret[ctx] = (1..numberOfBins).collect {
                def lowerBound = min[ctx] + ((max[ctx] - min[ctx]) / numberOfBins) * (it - 1)
                def upperBound = min[ctx] + ((max[ctx] - min[ctx]) / numberOfBins) * it
                def op2 = it == numberOfBins ? '≤' : '<'
                "$lowerBound ≤ $header $op2 $upperBound" as String
            }
        }
    }()

    private Map<String, BigDecimal> inverseBinInterval = {
        def ret = [:]
        ret.withDefault { ctx ->
            ret[ctx] = numberOfBins / (max[ctx] - min[ctx])
        }
    }()


    private void consumeEntry(String pk, String ctx, Number value) {
        if (value < min[ctx]) {
            min[ctx] = value
        }
        if (value > max[ctx]) {
            max[ctx] = value
        }
        allResults.put pk, ctx, value
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
        if (allResults == null) { /* results already given */
            return ImmutableMap.of()
        }

        for (entry in inner.consumeResultingTableRows()) {
            assert entry.value != null /* otherwise violates contract of consumeRTR() */
            consumeEntry entry.key, '', entry.value
        }

        if (numSubscribedDataSources > 0) {
            return ImmutableMap.of()
        }

        def res = ImmutableMap.builder()

        if (max.keySet() == [''] as Set) {
            for (pk in allResults.rowKeySet() /* pks */) {
                res.put pk, doResultForPrimaryKeySimple(pk)
            }
        } else {
            for (pk in allResults.rowKeySet() /* pks */) {
                res.put pk, doResultForPrimaryKey(pk)
            }
        }

        allResults = null
        res.build()
    }

    private String transform(String ctx, Number value) {
        /* normalize to interval [0, numberOfBins] */
        def norm = (value - min[ctx]) * inverseBinInterval[ctx]
        def bin = (norm as int)
        assert bin >= 0
        if (bin == numberOfBins) { //happens for max
            bin--
        }

        binNames[ctx][bin]
    }

    private Map doResultForPrimaryKey(String pk) {
        ImmutableMap.Builder<String, String> res = ImmutableMap.builder()
        for (Map.Entry<String, Object> entry in allResults.row(pk)) {
            res.put(entry.key /* ctx */, transform(entry.key, entry.value))
        }
        res.build()
    }

    private String doResultForPrimaryKeySimple(String pk) {
        transform '', allResults.get(pk, '')
    }


    void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
        --numSubscribedDataSources
        inner.onDataSourceDepleted dataSourceName, dataSource
    }

}
