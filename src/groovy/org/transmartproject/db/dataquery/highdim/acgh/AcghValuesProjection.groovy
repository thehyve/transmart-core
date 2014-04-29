package org.transmartproject.db.dataquery.highdim.acgh

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.acgh.AcghValues
import org.transmartproject.core.dataquery.highdim.acgh.CopyNumberState
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

class AcghValuesProjection implements CriteriaProjection<AcghValues>, MultiValueProjection {

    Map<String, Class> dataProperties = AcghValues.metaClass.properties.collectEntries {
        it.name != 'class' ? [(it.name): it.type] : [:]
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // AcghModule.prepareDataQuery already projects all the columns
    }

    @Override
    AcghValues doWithResult(Object object) {
        new AcghValuesImpl(object)
    }

    class AcghValuesImpl implements AcghValues {
        final List rowList

        AcghValuesImpl(final List rowList) {
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
}
