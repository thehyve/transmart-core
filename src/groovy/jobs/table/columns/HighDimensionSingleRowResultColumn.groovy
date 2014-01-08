package jobs.table.columns

import com.google.common.collect.ImmutableMap
import groovy.util.logging.Log4j
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.UnexpectedResultException

@Log4j
class HighDimensionSingleRowResultColumn extends AbstractColumn {

    final boolean globalComputation = false

    private DataRow row

    private List<AssayColumn> assays

    @Override
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        assert dataSource instanceof TabularResult

        assays = ((TabularResult)dataSource).indicesList
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        assert row instanceof DataRow

        if (this.row) {
            log.warn("Further rows from $dataSourceName ignored")
            return
        }

        this.row = row
    }

    @Override
    Map<String, String> consumeResultingTableRows() {
        if (!row) return ImmutableMap.of()

        ImmutableMap.Builder builder = ImmutableMap.builder()
        assays.each {
            builder.put(it.patientInTrialId, (row[it] ?: '') as String)
        }
        row = null
        builder.build()
    }

}
