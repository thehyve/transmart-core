package org.transmartproject.db.dataquery.highdim.rnaseq

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

class RnaSeqValuesProjection implements CriteriaProjection<RnaSeqValues>, MultiValueProjection {

    Collection<String> dataProperties = RnaSeqValues.metaClass.properties*.name - 'class'

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // RnaSeqModule.prepareDataQuery already projects all the columns
    }

    @Override
    RnaSeqValues doWithResult(Object object) {
        new RnaSeqValuesImpl(object)
    }

    class RnaSeqValuesImpl implements RnaSeqValues {
        final List rowList

        RnaSeqValuesImpl(final List rowList) {
            this.rowList = rowList
        }

        Long getAssayId() { rowList[0] as Long }

        Integer getReadCount() { rowList[1] as Integer }
    }
}
