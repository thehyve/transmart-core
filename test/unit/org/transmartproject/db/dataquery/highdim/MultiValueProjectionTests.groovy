package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghValuesProjection
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqValuesProjection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder

@TestMixin(GrailsUnitTestMixin)
class MultiValueProjectionTests {

    @Test
    void testAllDataProjectionProperties() {
        List<String> dataProps = ['foo', 'bar']
        List<String> rowProps = ['rowA', 'rowB']

        AllDataProjectionFactory factory = new AllDataProjectionFactory(dataProps, rowProps)
        AllDataProjection projection = factory.createFromParameters(Projection.ALL_DATA_PROJECTION, [:], null)

        assertThat projection.dataProperties, containsInAnyOrder(dataProps.toArray())
    }

    @Test
    void testAcghProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        List<String> dataProps = [
                'probabilityOfNormal',
                'probabilityOfAmplification',
                'copyNumberState',
                'segmentCopyNumberValue',
                'probabilityOfGain',
                'chipCopyNumberValue',
                'probabilityOfLoss']

        AcghValuesProjection projection = new AcghValuesProjection()
        assertThat projection.dataProperties, containsInAnyOrder(dataProps.toArray())
    }

    @Test
    void testRnaSeqProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        List<String> dataProps = ['readCount']

        RnaSeqValuesProjection projection = new RnaSeqValuesProjection()
        assertThat projection.dataProperties, containsInAnyOrder(dataProps.toArray())
    }

}
