package org.transmartproject.batch.highdim.assays

import org.junit.Test
import org.transmartproject.batch.concept.ConceptFragment

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains

/**
 * Tests for {@link MappingFileRow}
 */
class MappingFileRowTests {

    @Test
    void testCurrentPlaceholders() {
        def mappingFileRow = new MappingFileRow(
                platform: 'Test Platform',
                tissueType: 'Skin',
                sampleType: 'Test sample type',
                timePoint: 'Week1',
                categoryCd: 'Data+PLATFORM+TISSUETYPE+SAMPLETYPE+TIMEPOINT+Leaf'
        )

        ConceptFragment conceptFragment = mappingFileRow.conceptFragment

        assertThat conceptFragment.parts, contains(
                'Data',
                mappingFileRow.platform,
                mappingFileRow.tissueType,
                mappingFileRow.sampleType,
                mappingFileRow.timePoint,
                'Leaf')
    }

    @Test
    void testReplacesAttr1LegacyPlaceholder() {
        def mappingFileRow = new MappingFileRow(tissueType: 'Skin', categoryCd: 'Data+ATTR1')

        ConceptFragment conceptFragment = mappingFileRow.conceptFragment

        assertThat conceptFragment.parts, contains('Data', mappingFileRow.tissueType)
    }

    @Test
    void testReplacesAttr2LegacyPlaceholder() {
        def mappingFileRow = new MappingFileRow(timePoint: 'Baseline', categoryCd: 'Data+ATTR2+Leaf')

        ConceptFragment conceptFragment = mappingFileRow.conceptFragment

        assertThat conceptFragment.parts, contains('Data', mappingFileRow.timePoint, 'Leaf')
    }
}
