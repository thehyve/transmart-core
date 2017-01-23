package jobs.steps.helpers

import jobs.table.columns.AbstractColumn

class StubColumn extends AbstractColumn {
    Map<String, String> data

    @Override
    void onReadRow(String dataSourceName, Object row) {}

    @Override
    Map<String, String> consumeResultingTableRows() {
        try {
            return data
        } finally {
            data = null
        }
    }
}
