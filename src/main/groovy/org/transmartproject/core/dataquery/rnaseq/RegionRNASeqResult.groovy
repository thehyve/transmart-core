package org.transmartproject.core.dataquery.rnaseq

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.DataQueryResult

/**
 * The {@link DataQueryResult} type used for RNASeq region queries.
 */
public interface RegionRNASeqResult extends DataQueryResult<Assay, RegionRNASeqRow> {}
