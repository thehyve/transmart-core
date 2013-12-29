package jobs.table.columns

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.UnexpectedResultException

class HighDimensionSingleRowResultColumn extends AbstractColumn {

    final boolean globalComputation = false

    private DataRow row

    List<AssayColumn> assays

    @Override
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        assert dataSource instanceof TabularResult

        assays = ((TabularResult)dataSource).indicesList
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        assert row instanceof DataRow

        if (this.row) {
            throw new UnexpectedResultException('Expected only one row')
        }

        this.row = row
    }

    @Override
    Map<String, String> consumeResultingTableRows() {
        ImmutableMap.Builder builder = ImmutableMap.builder()

        assays.each {
            builder.put(it.patientInTrialId, (row[it] ?: '') as String)
        }
        builder.build()
    }

}
