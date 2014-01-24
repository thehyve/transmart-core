package jobs.table.columns

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.ForwardingMap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn

/**
 * Exposes a {@link org.transmartproject.core.dataquery.DataRow} as a
 * {@link Map}. The keys are the patient ids, the values are maps with
 * a single entry: row label -> value
 */
@CompileStatic
class HighDimensionalDataRowMapAdapter extends ForwardingMap<String, Map<String, Object>> {

    Map<String, Map<String, Object>> innerMap

    HighDimensionalDataRowMapAdapter(List<AssayColumn> assays /* in correct order! */,
                                     DataRow<AssayColumn, ?> row) {

        /* empty cells are dropped!
         * In the future we may want to provide a MissingValueAction
         * to this class in order to customize this behavior */


        Map<String, Integer> patientIdtoAssayIndex =
                Maps.uniqueIndex((0..(assays.size() - 1)),
                        { Integer i ->
                            assays[i].patientInTrialId
                        } as Function)

        Map<String, Map<String, Object>> patientIdToDataValue =
                Maps.transformValues patientIdtoAssayIndex,
                        { Integer i ->
                            def value = row.getAt(i)
                            if (value == null) {
                                return null
                            }
                            ImmutableMap.of(row.label, value)
                        } as Function

        // drop nulls
        innerMap = Maps.filterValues patientIdToDataValue,
                { Object value -> value != null } as Predicate
    }

    @Override
    protected Map<String, Map<String, Object>> delegate() {
        innerMap
    }
}
