package jobs.table.columns.binning

import com.google.common.collect.ImmutableMap
import jobs.table.Column
import jobs.table.columns.CategoricalVariableColumn
import jobs.table.columns.ColumnDecorator
import org.transmartproject.core.exceptions.InvalidArgumentsException

class CategoricalBinningColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    /* concept path (not value, which is ignored!) -> bin name */
    Map<String, String> transformationMap

    @Override
    Map<String, Object> consumeResultingTableRows() {
        CategoricalVariableColumn castInner = inner
        if (!castInner.lastRow) return ImmutableMap.of()

        for (clinicalVariable in castInner.leafNodes) {
            if (castInner.lastRow[clinicalVariable]) {
                def newValue = transformationMap[clinicalVariable.label /* concept path */]
                if (!newValue) {
                    throw new IllegalStateException("Binning invalidly configured. " +
                            "transformation map is $transformationMap, which does not " +
                            "contain the key $clinicalVariable.label")
                }

                return ImmutableMap.of(castInner.getPrimaryKey(castInner.lastRow),
                        newValue as String)
            }
        }

        ImmutableMap.of()
    }

    void setInner(Column column) {
        if (!(column instanceof CategoricalVariableColumn)) {
            throw new InvalidArgumentsException(
                    'Can only decorate categorical variable columns')
        }
        inner = column
    }
}
