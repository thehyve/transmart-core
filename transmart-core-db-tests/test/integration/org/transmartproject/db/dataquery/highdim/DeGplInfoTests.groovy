package org.transmartproject.db.dataquery.highdim

import org.junit.Test

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
                hasProperty('markerType', equalTo('generic')),
                hasProperty('title', equalTo('Test Generic Platform')),
                hasProperty('organism', equalTo('Homo Sapiens')),
                hasProperty('genomeReleaseId', equalTo('hg18')),
                hasProperty('annotationDate', equalTo(Date.parse('yyyy-MM-dd', '2013-05-03'))),
        )

        assertThat GenomeBuildNumber.forId(platform.genomeReleaseId), is(GenomeBuildNumber.GRCh36)
    }
}
