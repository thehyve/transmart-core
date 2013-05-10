package org.transmartproject.db.highdim

import org.junit.*
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.acgh.Region

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Mixin(HighDimTestData)
class DeChromosomalRegionTests {

    @Before
    void setUp() {
        assertThat testRegionPlatform.save(), isA(Platform)
        assertThat testRegions*.save(), contains(
                isA(Region), isA(Region)
        )
    }


    @Test
    void testBasicDataFetch() {
        Region r = DeChromosomalRegion.get(testRegions[0].id)

        assertThat r, allOf(
                is(notNullValue()),
                hasProperty('chromosome', equalTo('1')),
                hasProperty('start', equalTo(33L)),
                hasProperty('end', equalTo(9999L)),
                hasProperty('numberOfProbes', equalTo(42)),
                hasProperty('name', equalTo('region 1:33-9999'))
        )
    }

    @Test
    void testGetPlatform() {
        Region r = DeChromosomalRegion.get(testRegions[0].id)

        assertThat r, is(notNullValue())

        assertThat r.platform, allOf(
                is(notNullValue()),
                hasProperty('id', equalTo(testRegionPlatform.id))
        )
    }

}
