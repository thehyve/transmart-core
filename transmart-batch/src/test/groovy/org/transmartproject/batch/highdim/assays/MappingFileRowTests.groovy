package org.transmartproject.batch.highdim.assays

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.transmartproject.batch.concept.ConceptFragment

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.equalTo

/**
 * Tests for {@link MappingFileRow}
 */
class MappingFileRowTests {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException expectedException = ExpectedException.none()

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

    @Test
    void testUnderscoresReplacedBySpaces() {
        def mappingFileRow = new MappingFileRow(categoryCd: 'A_b+C_d\\E f')

        ConceptFragment conceptFragment = mappingFileRow.conceptFragment

        assertThat conceptFragment.parts, contains('A b', 'C d', 'E f')
    }

    @Test
    void testFailOnEmptyNodeNames() {
        String path = 'A\\\\B'
        def mappingFileRow = new MappingFileRow(categoryCd: path)

        expectedException.expect(IllegalArgumentException)
        expectedException.expectMessage(equalTo("Path cannot have empty parts (got '${path}')".toString()))

        mappingFileRow.conceptFragment
    }
}
