package org.transmartproject.db.dataquery

import groovy.transform.CompileStatic
import org.hibernate.Query
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.StatelessSession
import org.hibernate.impl.AbstractSessionImpl
import org.transmartproject.core.dataquery.DataQueryResource
import org.transmartproject.core.dataquery.acgh.RegionResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery
import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.acgh.ACGHValues
import org.transmartproject.core.dataquery.acgh.CopyNumberState
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.acgh.RegionRow

import static org.hibernate.ScrollMode.FORWARD_ONLY

class DataQueryResourceNoGormService extends DataQueryResourceService {

    @Override
    protected RegionResult getRegionResultForAssays(final List<Assay> assays, AbstractSessionImpl session) {
        def mainHQL = '''
            select
                acgh.assay.id,
                acgh.chipCopyNumberValue,
                acgh.segmentCopyNumberValue,
                acgh.flag,
                acgh.probabilityOfLoss,
                acgh.probabilityOfNormal,
                acgh.probabilityOfGain,
                acgh.probabilityOfAmplification,

                region.id,
                region.cytoband,
                region.chromosome,
                region.start,
                region.end,
                region.numberOfProbes
            from DeSubjectAcghData as acgh
            inner join acgh.region region
            where acgh.assay.id in (:assayIds)
            order by region.id, acgh.assay.id'''.stripIndent()

        def mainQuery = createQuery(session, mainHQL, ['assayIds': assays.collect {Assay assay -> assay.id}]).scroll(FORWARD_ONLY)

        new RegionResultListImpl(assays, mainQuery)
    }

    @CompileStatic
    class ACGHValuesImpl implements ACGHValues {
        final List rowList

        ACGHValuesImpl(final List rowList) {
            this.rowList = rowList
        }

        Long getAssayId() { rowList[0] as Long }

        Double getChipCopyNumberValue() { rowList[1] as Double }

        Double getSegmentCopyNumberValue() { rowList[2] as Double }

        CopyNumberState getCopyNumberState() { CopyNumberState.forInteger((rowList[3] as Short).intValue()) }

        Double getProbabilityOfLoss() { rowList[4] as Double }

        Double getProbabilityOfNormal() { rowList[5] as Double }

        Double getProbabilityOfGain() { rowList[6] as Double }

        Double getProbabilityOfAmplification() { rowList[7] as Double }

    }

    @CompileStatic
    class RegionImpl implements Region {

        final List rowList

        RegionImpl(final List rowList) {
            this.rowList = rowList
        }

        Long getId() { rowList[8] as Long }

        String getCytoband() { rowList[9] as String }

        Platform getPlatform() {
            throw new UnsupportedOperationException('Getter for get platform is not implemented')
        }

        String getChromosome() { rowList[10] as String }

        Long getStart() { rowList[11] as Long }

        Long getEnd() { rowList[12] as Long }

        Integer getNumberOfProbes() { rowList[13] as Integer }

    }

    @CompileStatic
    class RegionResultListImpl extends org.transmartproject.db.dataquery.RegionResultImpl {
        RegionResultListImpl(List<Assay> indicesList, ScrollableResults results) {
            super(indicesList, results)
        }

        @Override
        protected RegionRow getNextRegionRow() {
            List rowList = results.get() as List
            if (rowList == null) {
                return null
            }

            RegionImpl region = new RegionImpl(rowList)
            ACGHValuesImpl acghValue = new ACGHValuesImpl(rowList)

            Map values = new HashMap(indicesList.size(), 1)
            while (new RegionImpl(rowList).id == region.id) {
                values[acghValue.assayId] = acghValue

                if (!results.next()) {
                    break
                }
                rowList = results.get() as List
                acghValue = new ACGHValuesImpl(rowList)
            }

            new RegionRowImpl(region, indicesList, values)
        }
    }
}


