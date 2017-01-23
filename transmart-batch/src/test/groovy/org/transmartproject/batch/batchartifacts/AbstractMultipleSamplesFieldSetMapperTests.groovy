package org.transmartproject.batch.batchartifacts

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.springframework.batch.item.file.transform.DefaultFieldSet
import org.springframework.batch.test.MetaDataInstanceFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link AbstractMultipleVariablesPerSampleFieldSetMapper} class.
 */
class AbstractMultipleSamplesFieldSetMapperTests {

    private static final double DELTA = 0.005d

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    AbstractMultipleVariablesPerSampleFieldSetMapper testee

    void init(parsedHeaderMap) {
        testee =
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
        testee.stepExecution = MetaDataInstanceFactory.createStepExecution()
        testee.stepExecution.executionContext
                .put(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY, parsedHeaderMap)
    }

    @Test
    void testSingleSample() {
        init([
                's1.dfld': [sample: 's1', suffix: 'dfld'],
                's1.ifld': [sample: 's1', suffix: 'ifld'],
        ])
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
        init([
                's1.dfld': [sample: 's1', suffix: 'dfld'],
                's1.ifld': [sample: 's1', suffix: 'ifld'],
                's2.dfld': [sample: 's2', suffix: 'dfld'],
                's2.ifld': [sample: 's2', suffix: 'ifld'],
        ])
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
        init([
                's1.dfld': [sample: 's1', suffix: 'dfld'],
                's1.ifld': [sample: 's1', suffix: 'ifld'],
        ])
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

    @Test
    void testUnknownVariable() {
        init([
                's1.dfld'   : [sample: 's1', suffix: 'dfld'],
                's1.unknown': [sample: 's1', suffix: 'unknown'],
        ])
        exception.expect(UnsupportedOperationException)
        exception.expectMessage(
                equalTo('Variable unknown is not supported.'))

        def fieldSet = new DefaultFieldSet(['test-annot', '1.2', 'doesn\'t matter'] as String[],
                ['annot', 's1.dfld', 's1.unknown'] as String[])

        testee.mapFieldSet(fieldSet)
    }

    @Test
    void testNotParsedColumn() {
        init([:])
        exception.expect(IllegalStateException)
        exception.expectMessage('Column unparsable has not been parsed.')

        def fieldSet = new DefaultFieldSet(['test-annot', '1.2'] as String[],
                ['annot', 'unparsable'] as String[])

        testee.mapFieldSet(fieldSet)
    }

}
