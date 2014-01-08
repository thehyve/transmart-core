package jobs.table.columns

import com.google.common.collect.ImmutableMap
import jobs.table.MissingValueAction

class ConstantValueColumn extends AbstractColumn {

    void setValue(String value) {
        missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: value)
    }

    @Override
    boolean isGlobalComputation() {
        false
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* purposefully left empty */
    }

    @Override
    Map<String, String> consumeResultingTableRows() {
        ImmutableMap.of()
    }
}
