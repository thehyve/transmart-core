package org.transmartproject.db.dataquery.highdim

import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.db.dataquery.CollectingTabularResult

@ToString
class DefaultHighDimensionTabularResult<R extends DataRow>
        extends CollectingTabularResult<AssayColumn, R> {

    final String columnEntityName = 'assay'

    /* aliases */
    public void setAllowMissingAssays(Boolean value) {
        allowMissingColumns = value
    }

    public void setAssayIdFromRow(Closure<Object> value) {
        columnIdFromRow = value
    }
}
