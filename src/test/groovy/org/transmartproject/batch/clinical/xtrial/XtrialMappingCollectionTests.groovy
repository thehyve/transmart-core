package org.transmartproject.batch.clinical.xtrial

import org.junit.Before
import org.junit.Test
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Unit tests for {@link XtrialMappingCollection}.
 */
class XtrialMappingCollectionTests {

    private final static ConceptPath TOP_NODE =
            new ConceptPath('\\Public Studies\\Foo bar\\')

    XtrialMappingCollection testee = new XtrialMappingCollection(
            topNode: TOP_NODE
    )

    @Before
    void setupTestee() {
        [
                ['\\f\\',       '\\a\\b\\'],
                ['\\f\\g\\',    '\\b\\c\\'],
                ['\\f\\g\\h\\', '\\a\\'],
                ['\\i\\',       '\\b\\c\\d\\'],
        ].each {
            testee.registerUserMapping(new XtrialMapping(
                    studyPrefix: it[0],
                    xtrialPrefix: it[1],
            ))
        }

        def c = { new ConceptFragment(it) }
        int i = 0
        [
                new XtrialNode(path: c('\\a\\'), type: ConceptType.CATEGORICAL),
                new XtrialNode(path: c('\\a\\b\\'), type: ConceptType.CATEGORICAL),
                new XtrialNode(path: c('\\a\\b\\x\\'), type: ConceptType.CATEGORICAL),
                new XtrialNode(path: c('\\a\\b\\x\\y\\'), type: ConceptType.CATEGORICAL),
                // '\\b\\c\\ does not exist
                new XtrialNode(path: c('\\b\\c\\d\\'), type: ConceptType.NUMERICAL),
        ].each {
            it.code = i++ as String
            testee.registerTrialNode it
        }
    }

    @Test
    void testDisjunctXtrialPrefix() {
        assertThat testee.disjunctXtrialPrefixes, containsInAnyOrder(
                new ConceptFragment('\\a\\'),
                new ConceptFragment('\\b\\c\\'),
        )
    }

    @Test
    void testFindMappedXtrialNodeBasic() {
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\f\\x\\y\\', ConceptType.CATEGORICAL)
        assertThat res, hasProperty('path', equalTo(new ConceptFragment('\\a\\b\\x\\y\\')))
    }

    @Test
    void testFindMappedXtrialNodeTopNode() {
        def res = testee.findMappedXtrialNode(TOP_NODE, ConceptType.CATEGORICAL)
        assertThat res, is(nullValue())
    }

    @Test
    void testNodeMappedDirectly() {
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\f\\', ConceptType.CATEGORICAL)
        assertThat res, hasProperty('path', equalTo(new ConceptFragment('\\a\\b\\')))
    }

    @Test
    void testUnmappedNodeOrderedBeforeAll() {
        // test with a study fragment that orders before all the mappings we have
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\e\\', ConceptType.CATEGORICAL)
        assertThat res, is(nullValue())
    }

    @Test
    void testUnmappedNodeOrderedAfterSomeMapping() {
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\ff\\', ConceptType.CATEGORICAL)
        assertThat res, is(nullValue())
    }

    @Test
    void testMoreSpecificMappingsHavePrecedence() {
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\f\\g\\d\\', ConceptType.NUMERICAL)
        assertThat res, hasProperty('path', equalTo(new ConceptFragment('\\b\\c\\d\\')))
    }

    @Test
    void testTypeMismatch() {
        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\f\\g\\d\\', ConceptType.CATEGORICAL)
        assertThat res, is(nullValue())
    }

    @Test
    void testTrivialMapping() {
        testee.registerUserMapping(new XtrialMapping(
                studyPrefix: new ConceptFragment('\\'),
                xtrialPrefix: new ConceptFragment('\\')
        ))

        XtrialNode res = testee.findMappedXtrialNode(
                TOP_NODE + '\\a\\b\\', ConceptType.CATEGORICAL)
        assertThat res, hasProperty('path', equalTo(new ConceptFragment('\\a\\b\\')))
    }

}
