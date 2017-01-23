package jobs.table.columns

import com.google.common.collect.ImmutableMap
import jobs.table.BackingMap

class PrimaryKeyColumn extends AbstractColumn {

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* don't care */
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        ImmutableMap.of()
    }

    @Override
    void onAllDataSourcesDepleted(int columnNumber, BackingMap backingMap) {
        backingMap.primaryKeys.each { pk ->
            backingMap.putCell pk, columnNumber, pk
        }
    }
}
