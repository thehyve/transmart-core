package org.transmartproject.db.dataquery.highdim

import org.junit.Test
import org.transmartproject.core.dataquery.highdim.PlatformMarkerType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeGplInfoTests {

    SampleHighDimTestData testData = new SampleHighDimTestData()

    @Test
    void testScalarProperties() {
        assertThat testData.platform.save(), is(notNullValue())

        def platform = DeGplInfo.get(testData.platform.id)

        assertThat platform, allOf(
                is(notNullValue()),
                hasProperty('title', equalTo('Test Generic Platform')),
                hasProperty('organism', equalTo('Homo Sapiens')),
                hasProperty('releaseNumber', equalTo(18)),
                hasProperty('annotationDate', equalTo(
                        Date.parse('yyyy-MM-dd', '2013-05-03'))),

        )
    }

    @Test
    void testGetMarkerType() {
        def platform1 = new DeGplInfo(markerTypeId: 'Chromosomal Region')
        platform1.id = 'test_platform_1'
        def platform2 = new DeGplInfo(markerTypeId: 'foobar')
        platform2.id = 'test_platform_2'

        assertThat platform1.save(), is(notNullValue())
        assertThat platform2.save(), is(notNullValue())

        def regionPlatform = DeGplInfo.get(platform1.id)
        assertThat regionPlatform, allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo(PlatformMarkerType.CHROMOSOMAL_REGION))
        )

        def bogusPlatform = DeGplInfo.get(platform2.id)
        assertThat bogusPlatform, allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo(PlatformMarkerType.UNKNOWN))
        )
    }
}
