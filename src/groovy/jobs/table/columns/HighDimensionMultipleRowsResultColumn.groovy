package jobs.table.columns

import com.google.common.collect.Sets
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.UnexpectedResultException

class HighDimensionMultipleRowsResultColumn extends AbstractColumn {

    private List<AssayColumn> assays

    private DataRow<AssayColumn, ?> lastRow

    private Set<String> seenRowLabels = Sets.newHashSet()

    @Override
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        assert dataSource instanceof TabularResult

        assays = ((TabularResult)dataSource).indicesList
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* calls to onReadRow() and consumeResultingTableRows should be interleaved */
        assert row instanceof DataRow
        assert lastRow == null

        String rowLabel = row.label
        if (!seenRowLabels.add(rowLabel)) {
            throw new UnexpectedResultException(
                    "Got more than one row with label $rowLabel")
        }

        lastRow = row
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        assert lastRow != null

        def row = lastRow
        lastRow = null
        /* empty cells are dropped; see HighDimensionalDataRowMapAdapter */
        new HighDimensionalDataRowMapAdapter(assays, row)
    }


}
