package org.transmartproject.batch.batchartifacts

import org.junit.Test
import org.springframework.batch.item.file.transform.DefaultFieldSet

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link AbstractMultipleVariablesPerSampleFieldSetMapper} class.
 */
class AbstractMultipleSamplesFieldSetMapperTests {

    private static final double DELTA = 0.005d

    AbstractMultipleVariablesPerSampleFieldSetMapper testee =
            new AbstractMultipleVariablesPerSampleFieldSetMapper<TestBean>() {

                @Override
                TestBean newInstance(String annotation, String sampleCode) {
                    new TestBean(annotation: annotation, sampleCode: sampleCode)
                }

                @Override
                Map<String, Closure> getFieldSetters() {
                    [
                            dfld: { TestBean bean, String value -> bean.doubleField = value as Double },
                            ifld: { TestBean bean, String value -> bean.intField = value as Integer },
                    ]
                }
            }

    @Test
    void testSingleSample() {
        def fieldSet = new DefaultFieldSet(['test-annot', '23.45', '10'] as String[],
                ['annot', 's1.dfld', 's1.ifld'] as String[])
        def items = testee.mapFieldSet(fieldSet)

        assertThat items, contains(allOf(
                hasProperty('annotation', equalTo('test-annot')),
                hasProperty('sampleCode', equalTo('s1')),
                hasProperty('doubleField', closeTo(23.45d, DELTA)),
                hasProperty('intField', equalTo(10)),
        ))
    }

    @Test
    void testMultipleSamples() {
        def fieldSet = new DefaultFieldSet(['test-annot', '12.13', '22', '35.67', '19'] as String[],
                ['annot', 's1.dfld', 's1.ifld', 's2.dfld', 's2.ifld'] as String[])
        def items = testee.mapFieldSet(fieldSet)

        assertThat items, contains(
                allOf(
                        hasProperty('annotation', equalTo('test-annot')),
                        hasProperty('sampleCode', equalTo('s1')),
                        hasProperty('doubleField', closeTo(12.13d, DELTA)),
                        hasProperty('intField', equalTo(22))),
                allOf(
                        hasProperty('annotation', equalTo('test-annot')),
                        hasProperty('sampleCode', equalTo('s2')),
                        hasProperty('doubleField', closeTo(35.67d, DELTA)),
                        hasProperty('intField', equalTo(19)),
                ))
    }

    @Test
    void testLoyalToNulls() {
        def fieldSet = new DefaultFieldSet([null, null, null] as String[],
                ['annot', 's1.dfld', 's1.ifld'] as String[])
        def items = testee.mapFieldSet(fieldSet)

        assertThat items, contains(allOf(
                hasProperty('annotation', nullValue()),
                hasProperty('sampleCode', equalTo('s1')),
                hasProperty('doubleField', nullValue()),
                hasProperty('intField', nullValue()),
        ))
    }

    @Test(expected = UnsupportedOperationException)
    void testUnknownVariable() {
        def fieldSet = new DefaultFieldSet(['test-annot', '1.2', 'doesn\'t matter'] as String[],
                ['annot', 's1.dfld', 's1.unknown'] as String[])

        testee.mapFieldSet(fieldSet)
    }

    @Test
    void testSampleWithDots() {
        def fieldSet = new DefaultFieldSet(['annot', '5'] as String[],
                ['annot', 'sample.could.contain.dots.ifld'] as String[])
        def items = testee.mapFieldSet(fieldSet)

        assertThat items, contains(allOf(
                hasProperty('annotation', equalTo('annot')),
                hasProperty('sampleCode', equalTo('sample.could.contain.dots')),
                hasProperty('intField', equalTo(5)),
        ))
    }

    @Test(expected = UnsupportedOperationException)
    void testCaseMatters() {
        def fieldSet = new DefaultFieldSet(['test-annot', '1.2'] as String[],
                ['annot', 's1.Dfld'] as String[])

        testee.mapFieldSet(fieldSet)
    }

}
