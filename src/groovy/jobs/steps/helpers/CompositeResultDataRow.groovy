package jobs.steps.helpers

import com.google.common.base.Function
import com.google.common.collect.Iterators
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn

class CompositeResultDataRow implements DataRow<AssayColumn, Number> {

    public static final String SEPARATOR = '|'

    DataRow<AssayColumn, Number> delegateRow

    List<AssayColumn> assayColumns

    String conceptPath

    @Override
    String getLabel() {
        conceptPath + SEPARATOR + delegateRow.label
    }

    @Override
    Number getAt(int index) {
        delegateRow.getAt(assayColumns[index])
    }

    @Override
    Number getAt(AssayColumn column) {
        delegateRow.getAt(column)
    }

    @Override
    Iterator<Number> iterator() {
        Iterators.transform(assayColumns.iterator(), this.&getAt as Function)
    }
}
