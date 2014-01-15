package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.DataRow

public interface BioMarkerDataRow<CELL> extends DataRow<AssayColumn, CELL> {

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

    /**
     * Some data types support datatype specific properties, which are needed for example when exporting the data. This property
     * @return
     */
    Map<String, Object> getAssociatedData()
}
