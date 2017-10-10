package org.transmartproject.core.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn

public interface RegionRow<V> extends Region, ColumnOrderAwareDataRow<AssayColumn, V> { }
