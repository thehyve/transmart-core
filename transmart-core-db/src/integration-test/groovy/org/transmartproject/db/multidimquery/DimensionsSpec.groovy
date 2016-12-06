package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.multidimquery.Dimension
import spock.lang.Specification

@Rollback
@Integration
class DimensionsSpec extends Specification {

    def 'studyDim.resolveElements guarantees order'() {
        expect:
        resolveElementsGuaranteesOrder(DimensionImpl.dimensionsMap.study, [
                -20,
                -31,
                -30,
                -22,
                -27,
                -28,
                -23,
                -29,
                -21,
        ])
    }

    def 'conceptDim.resolveElements guarantees order'() {
        expect:
        resolveElementsGuaranteesOrder(DimensionImpl.dimensionsMap.concept, [
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
