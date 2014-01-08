package jobs.table.columns.binning

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.Maps
import jobs.table.Column
import jobs.table.columns.ColumnDecorator

class NumericManualBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    List<NumericBinRange> binRanges

    private @Lazy List binNames = {
        def ret = []
        for (i in 0..(binRanges.size() - 1)) {
            def op1
            def lowerBound
            if (i == 0) {
                op1 = '≤'
                lowerBound = binRanges[0].from
            } else if (binRanges[i].from == binRanges[i - 1].to) {
                op1 = '<'
                lowerBound = binRanges[i].from
            } else if (binRanges[i].from > binRanges[i - 1].to) {
                op1 = '≤'
                lowerBound = binRanges[i].from
            } else {
                throw new IllegalStateException("binRanges[$i].from < " +
                        "binRanges[${i - 1}].from. Bad column definition")
            }

            ret << "$lowerBound $op1 $header ≤ ${binRanges[i].to}".toString()
        }
        ret
    }()

    @Override
    Map<String, String> consumeResultingTableRows() {
        Map transformedValues = Maps.transformValues(
                inner.consumeResultingTableRows(),
                this.&transformValue as Function)

        Maps.filterValues transformedValues, { it != null } as Predicate
    }

    private String transformValue(String originalValue) {
        def numericValue = originalValue as BigDecimal

        /* not the most efficient implementation... */
        for (i in 0..(binRanges.size() - 1)) {
            if (numericValue >= binRanges[i].from && numericValue <= binRanges[i].to) {
                return binNames[i]
            }
        }
    }

}
