package org.transmartproject.db.dataquery.highdim.snp_lz

import com.google.common.collect.ImmutableMap
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.isA

class SnpLzAllDataProjectionTests {

    @Test
    void testAllDataProjectionPropertiesOrder() {
        AllDataProjection proj = new SnpLzAllDataProjection()
        assertThat proj, isA(AllDataProjection)

        Map<String, Class> dataProperties = proj.dataProperties
        Map<String, Class> rowProperties = proj.rowProperties

        // Ensure that data/rowProperties have the expected iteration order
        assertThat dataProperties.keySet(), contains('probabilityA1A1', 'probabilityA1A2',
                'probabilityA2A2', 'likelyGenotype', 'minorAlleleDose')
        assertThat rowProperties.keySet(), contains('snpName', 'chromosome', 'position', 'a1', 'a2',
                'imputeQuality', 'GTProbabilityThreshold', 'minorAlleleFrequency', 'minorAllele', 'a1a1Count',
                'a1a2Count', 'a2a2Count', 'noCallCount')
    }
}
