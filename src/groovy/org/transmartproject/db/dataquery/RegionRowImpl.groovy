package org.transmartproject.db.dataquery

import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.acgh.ACGHValues
import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.acgh.RegionRow
import org.transmartproject.core.dataquery.assay.Assay

@CompileStatic
class RegionRowImpl implements RegionRow {

    final Region region
    final List<Assay> assayList
    final Map<Long, ACGHValues> values

    /**
     * Constructor.
     *
     * @param region the region this object provides values for
     * @param assayList list to map indexes to assays
     * @param values values map indexed by assay id
     */
    protected RegionRowImpl(Region region,
                            List<Assay> assayList,
                            Map<Long, ACGHValues> values) {
        this.region    = region
        this.assayList = assayList
        this.values    = values
    }

    @Override
    ACGHValues getAt(int index) throws ArrayIndexOutOfBoundsException {
        def assay = assayList[index]
        if (assay == null) {
            throw new ArrayIndexOutOfBoundsException("Invalid index: $index")
        }
        getRegionDataForAssay(assay)
    }

    @Override
    ACGHValues getRegionDataForAssay(Assay assay) throws ArrayIndexOutOfBoundsException {
        ACGHValues result = values[assay.id]
        if(!result) {
            def ids = assayList.collect { Assay assayp -> assayp.id }
            throw new ArrayIndexOutOfBoundsException("""Assay with id $assay.id
                    is not a valid index for this row; valid indexes are
                    ${values.keySet()}, which should match $ids""")
        }
        result
    }
}
