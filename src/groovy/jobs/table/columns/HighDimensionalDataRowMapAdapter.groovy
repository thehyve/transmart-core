package jobs.table.columns

import com.google.common.base.Function
import com.google.common.base.Predicate
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
class HighDimensionalDataRowMapAdapter implements Map {

    @Delegate
    Map innerMap

    HighDimensionalDataRowMapAdapter(List<AssayColumn> assays /* in correct order! */,
                                     DataRow<AssayColumn, ?> row) {

        /* empty cells are dropped!
         * In the future we may want to provide a MissingValueAction
         * to this class in order to customize this behavior */

         innerMap = Maps.filterValues(
                 (Map) Maps.transformValues(
                        Maps.uniqueIndex(
                                (0..(assays.size() - 1)) /* temp values */,
                                { Integer i ->
                                    assays[i].patientInTrialId
                                } as Function), /* temp value to key */
                        { Integer i ->
                            def value = row.getAt(i)
                            if (value == null) {
                                return null
                            }
                            ImmutableMap.of(row.label, value)
                        } as Function), /* replace temp value (index) w/ value */
                { Object value ->
                    value != null
                } as Predicate) /* remove nulls */
    }

}
