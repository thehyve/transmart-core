package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow

public interface BioMarkerDataRow<CELL> extends ColumnOrderAwareDataRow<AssayColumn, CELL> {

    /**
     * An arbitrary representation for an arbitrary biomarker. This is useful
     * for analyses such as the "marker selection" in RModules, which need to
     * have information about the biomarker associated with which row.
     *
     * The label of the row cannot generally be used for this purpose because
     * the label should uniquely identify the row, where multiple rows (or no
     * rows) can be can be associated with one biomarker.
     */
    String getBioMarker()
}
