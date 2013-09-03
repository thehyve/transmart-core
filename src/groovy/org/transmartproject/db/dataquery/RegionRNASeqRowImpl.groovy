package org.transmartproject.db.dataquery

import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.rnaseq.RNASEQValues
import org.transmartproject.core.dataquery.rnaseq.RegionRNASeqRow
import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.assay.Assay

@CompileStatic
class RegionRNASeqRowImpl implements RegionRNASeqRow {

    final Region region
    final List<Assay> assayList
    final Map<Long, RNASEQValues> values

    /**
     * Constructor.
     *
     * @param region the region this object provides values for
     * @param assayList list to map indexes to assays
     * @param values values map indexed by assay id
     */
    protected RegionRNASeqRowImpl(Region region,
                            List<Assay> assayList,
                            Map<Long, RNASEQValues> values) {
        this.region    = region
        this.assayList = assayList
        this.values    = values
    }

    @Override
    RNASEQValues getAt(int index) throws ArrayIndexOutOfBoundsException {
        def assay = assayList[index]
        if (assay == null) {
            throw new ArrayIndexOutOfBoundsException("Invalid index: $index")
        }
        getRegionDataForAssay(assay)
    }

    @Override
    RNASEQValues getRegionDataForAssay(Assay assay) throws ArrayIndexOutOfBoundsException {
        RNASEQValues result = values[assay.id]
        if(!result) {
            def ids = assayList.collect { Assay assayp -> assayp.id }
            throw new ArrayIndexOutOfBoundsException("""Assay with id $assay.id
                    is not a valid index for this row; valid indexes are
                    ${values.keySet()}, which should match $ids""")
        }
        result
    }
}
