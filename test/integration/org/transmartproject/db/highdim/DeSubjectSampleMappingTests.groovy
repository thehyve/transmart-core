package org.transmartproject.db.highdim

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.assay.SampleType
import org.transmartproject.core.dataquery.assay.Timepoint
import org.transmartproject.core.dataquery.assay.TissueType
import org.transmartproject.db.i2b2data.PatientDimension

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Mixin(HighDimTestData)
class DeSubjectSampleMappingTests {

    def sessionFactory

    @Before
    void setUp() {
        assertThat testRegionAssays*.save(), contains(
                isA(Assay), isA(Assay)
        )
        sessionFactory.currentSession.flush()

        System.out.println('Finished @Before')
    }

    @Test
    void testSimpleFetchScalarProperties() {
        def assay = DeSubjectSampleMapping.get(testRegionAssays[0].id)

        assertThat assay, allOf(
                is(notNullValue()),
                hasProperty('siteId', equalTo('site id #1')),
                hasProperty('conceptCode', equalTo('concept code #1')),
                hasProperty('trialName', equalTo('REGION_SAMP_TRIAL')),
                hasProperty('subjectId', equalTo('SUBJ_ID_1'))
        )
    }

    @Test
    void testOnDemandProperties() {
        // test the timepoint, sample and tissue properties
        def assay = DeSubjectSampleMapping.get(testRegionAssays[0].id)

        assertThat assay, allOf(
                is(notNullValue()),
                hasProperty('timepoint', equalTo(new Timepoint('timepoint ' +
                        'code', 'timepoint name #1'))),
                hasProperty('sampleType', equalTo(new SampleType('sample code',
                        'sample name #1'))),
                hasProperty('tissueType', equalTo(new TissueType('tissue code',
                        'tissue name #1'))),
        )

    }

    @Test
    void testReferences() {
        def assay = DeSubjectSampleMapping.get(testRegionAssays[0].id)

        assertThat assay, allOf(
                is(notNullValue()),
                hasProperty('patient', notNullValue(Patient)),
                hasProperty('platform', notNullValue(Platform)))

    }
}
