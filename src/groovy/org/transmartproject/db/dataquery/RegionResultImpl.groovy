package org.transmartproject.db.dataquery

import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.acgh.RegionResult
import org.transmartproject.core.dataquery.acgh.RegionRow
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.highdim.DeChromosomalRegion
import org.transmartproject.db.highdim.DeSubjectAcghData

@CompileStatic
class RegionResultImpl implements RegionResult, Closeable {

    final List<Assay> indicesList

    final ScrollableResults results


    RegionResultImpl(List<Assay> indicesList,
                     ScrollableResults results) {
        this.indicesList = indicesList
        this.results = results
    }

    protected RegionRow getNextRegionRow() {
        def entry = results.get()
        if (entry == null) {
            return null
        }

        DeSubjectAcghData v = entry[0] as DeSubjectAcghData
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
            v = results.get()[0] as DeSubjectAcghData
        }

        new RegionRowImpl(commonRegion, indicesList, values)
    }

    Iterator<RegionRow> getRows() {
        results.next() //go to first result
        def row = getNextRegionRow()

        [
                hasNext: { row != null },
                next: { def r = row; row = getNextRegionRow(); r },
                remove: { throw new UnsupportedOperationException() }
        ] as Iterator<RegionRow>
    }

    @Override
    void close() {
        results.close()
    }
}
