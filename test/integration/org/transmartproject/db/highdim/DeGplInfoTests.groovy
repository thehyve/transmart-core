package org.transmartproject.db.highdim

import org.junit.Test
import org.transmartproject.core.dataquery.PlatformMarkerType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Mixin(HighDimTestData)
class DeGplInfoTests {

    @Test
    void testScalarProperties() {
        assertThat testRegionPlatform.save(), is(notNullValue())

        def platform = DeGplInfo.get(testRegionPlatform.id)

        assertThat platform, allOf(
                is(notNullValue()),
                hasProperty('title', equalTo('Test Region Platform')),
                hasProperty('organism', equalTo('Homo Sapiens')),
                hasProperty('genomeBuild', equalTo('genome build #1')),
                hasProperty('annotationDate', equalTo(
                        Date.parse('yyyy-MM-dd', '2013-05-03'))),

        )
    }

    @Test
    void testGetMarkerType() {
        assertThat testRegionPlatform.save(), is(notNullValue())
        assertThat testBogusTypePlatform.save(), is(notNullValue())

        def regionPlatform = DeGplInfo.get(testRegionPlatform.id)
        assertThat regionPlatform, allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo(PlatformMarkerType.CHROMOSOMAL_REGION))
        )

        def bogusPlatform = DeGplInfo.get(testBogusTypePlatform.id)
        assertThat bogusPlatform, allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo(PlatformMarkerType.UNKNOWN))
        )
    }
}
