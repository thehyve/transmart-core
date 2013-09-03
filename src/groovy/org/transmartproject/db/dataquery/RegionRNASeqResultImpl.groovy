package org.transmartproject.db.dataquery

import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.rnaseq.RegionRNASeqResult
import org.transmartproject.core.dataquery.rnaseq.RegionRNASeqRow
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.highdim.DeChromosomalRegion
import org.transmartproject.db.highdim.DeSubjectRnaseqData

@CompileStatic
class RegionRNASeqResultImpl implements RegionRNASeqResult, Closeable {

    final List<Assay> indicesList

    final ScrollableResults results


    RegionRNASeqResultImpl(List<Assay> indicesList,
                     ScrollableResults results) {
        this.indicesList = indicesList
        this.results = results
    }

    protected RegionRNASeqRow getNextRegionRNASeqRow() {
        def entry = results.get()
        if (entry == null) {
            return null
        }

        DeSubjectRnaseqData v = entry[0] as DeSubjectRnaseqData
        DeChromosomalRegion commonRegion = entry[1] as DeChromosomalRegion

        Map values = new HashMap(indicesList.size())
        /* Use .@ to access the field and bypass the getter. The problem is
         * that gorm tries to unwrap the hibernate proxy in the getter and
         * this throws an exception on stateless sessions (even if using a
         * fetch join in the query; see the comment on
         * DataQueryResourceService).
         */
        while (v.@region.id == commonRegion.id) {
            values[v.@assay.id] = v

            if (!results.next()) {
                break
            }
            v = results.get()[0] as DeSubjectRnaseqData
        }

        new RegionRNASeqRowImpl(commonRegion, indicesList, values)
    }

    Iterator<RegionRNASeqRow> getRows() {
        results.next() //go to first result
        def row = getNextRegionRNASeqRow()

        [
                hasNext: { row != null },
                next: { def r = row; row = getNextRegionRNASeqRow(); r },
                remove: { throw new UnsupportedOperationException() }
        ] as Iterator<RegionRNASeqRow>
    }

    @Override
    void close() {
        results.close()
    }
}
