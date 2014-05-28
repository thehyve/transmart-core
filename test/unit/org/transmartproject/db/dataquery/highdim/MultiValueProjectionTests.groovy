package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.acgh.CopyNumberState
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghValuesProjection
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqValuesProjection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo


@TestMixin(GrailsUnitTestMixin)
class MultiValueProjectionTests {

    @Test
    void testAllDataProjectionProperties() {
        Map<String, Class> dataProps = [foo:String, bar:Double]
        Map<String, Class> rowProps = [rowA:Double, rowB:String]

        AllDataProjectionFactory factory = new AllDataProjectionFactory(dataProps, rowProps)
        AllDataProjection projection = factory.createFromParameters(Projection.ALL_DATA_PROJECTION, [:], null)

        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

    @Test
    void testAcghProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        Map<String, Class> dataProps = [
                probabilityOfNormal: Double,
                probabilityOfAmplification: Double,
                copyNumberState: CopyNumberState,
                segmentCopyNumberValue: Double,
                probabilityOfGain: Double,
                chipCopyNumberValue: Double,
                probabilityOfLoss: Double]

        AcghValuesProjection projection = new AcghValuesProjection()
        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

    @Test
    void testRnaSeqProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        Map<String, Class> dataProps = [readCount:Integer]

        RnaSeqValuesProjection projection = new RnaSeqValuesProjection()
        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

}
