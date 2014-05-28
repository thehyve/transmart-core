package org.transmartproject.db.dataquery.highdim.chromoregion

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeChromosomalRegionTests {

    AcghTestData testData = new AcghTestData()

    @Before
    void setUp() {
        assertThat testData.regionPlatform.save(), isA(Platform)
        assertThat testData.regions*.save(), contains(
                isA(Region), isA(Region)
        )
    }


    @Test
    void testBasicDataFetch() {
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

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
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

        assertThat r, is(notNullValue())

        assertThat r.platform, allOf(
                is(notNullValue()),
                hasProperty('id', equalTo(testData.regionPlatform.id))
        )
    }

}
