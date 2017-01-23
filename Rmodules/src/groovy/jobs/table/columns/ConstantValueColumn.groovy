package jobs.table.columns

import com.google.common.collect.ImmutableMap
import jobs.table.MissingValueAction

class ConstantValueColumn extends AbstractColumn {

    void setValue(Object value) {
        missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: value)
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* purposefully left empty */
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        ImmutableMap.of()
    }
}
