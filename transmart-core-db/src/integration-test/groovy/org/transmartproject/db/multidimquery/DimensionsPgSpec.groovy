/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.multidimquery.hypercube.Dimension
import spock.lang.Specification

@Rollback
@Integration
class DimensionsPgSpec extends Specification {

    def 'studyDim.resolveElements guarantees order'() {
        expect:
        resolveElementsGuaranteesOrder(DimensionImpl.STUDY, [
                'CATEGORICAL_VALUES',
                'CLINICAL_TRIAL',
                'CLINICAL_TRIAL_HIGHDIM',
                'EHR',
                'EHR_HIGHDIM',
                'MIX_HD',
                'SURVEY2',
                'ORACLE_1000_PATIENT',
                'RNASEQ_TRANSCRIPT',
                'TUMOR_NORMAL_SAMPLES',
                'SURVEY1'
        ])
    }

    def 'conceptDim.resolveElements guarantees order'() {
        expect:
        resolveElementsGuaranteesOrder(DimensionImpl.CONCEPT, [
                "CT:DEM:AGE",
                "CTHD:DEM:AGE",
                "CT:VSIGN:HR",
                "CTHD:HD:EXPBREAST",
                "CV:DEM:AGE",
                "CTHD:HD:EXPLUNG",
                "CTHD:VSIGN:HR",
                "CV:DEM:RACE",
        ])
    }


    void resolveElementsGuaranteesOrder(Dimension dim, List elementKeys) {
        List expectedOrder = elementKeys.collect { dim.resolveElement(it) }
        List actualOrder = dim.resolveElements(elementKeys)
        assert expectedOrder == actualOrder
    }

}
