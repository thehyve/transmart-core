package jobs.table.columns

import com.google.common.base.Function
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.UnexpectedResultException

class HighDimensionMultipleRowsResultColumn extends AbstractColumn {

    final boolean globalComputation = true

    /* pk (patient in trial id) -> row label, value
     * Unfortunately, we can't use an ImmutableTable and then call
     * rowMap() on onReadRow() because the ImmutableMap there returned,
     * while theoretically serializable (ImmutableMap implements Serializable)
     * fails when serializing due to a reference to the owning DenseImmutableTable,
     * which is not serializable.
     */
    private Map<String, ImmutableMap.Builder> results = {
        def ret = [:]
        ret.withDefault { key ->
            ret[key] = ImmutableMap.builder()
        }
    }()

    private List<AssayColumn> assays

    private boolean depleted = false

    private Set<String> seenRowLabels = Sets.newHashSet()

    @Override
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        assert dataSource instanceof TabularResult

        assays = ((TabularResult)dataSource).indicesList
    }

    @Override
    void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
        depleted = true
    }

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* non global computation, so calls to onReadRow()
         * and consumeResultingTableRows should be interleaved */
        assert row instanceof DataRow

        String rowLabel = row.label
        if (!seenRowLabels.add(rowLabel)) {
            throw new UnexpectedResultException(
                    "Got more than one row with label $rowLabel")
        }

        assays.each {
            def value = row[it]
            /* empty cells are dropped!
             * In the future we may want to provide a MissingValueAction
             * to this class in order to customize this behavior */
            if (value != null) {
                results[it.patientInTrialId].put rowLabel, value
            }
        }
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        if (results == null || !depleted) {
            return ImmutableMap.of()
        }

        def resultsLocal = results
        results = null
        Maps.transformValues(resultsLocal, { ImmutableMap.Builder builder ->
                builder.build()
        } as Function)
    }


}
