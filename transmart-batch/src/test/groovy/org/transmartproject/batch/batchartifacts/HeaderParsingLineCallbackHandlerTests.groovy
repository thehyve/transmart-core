package org.transmartproject.batch.batchartifacts

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.springframework.batch.core.StepExecution
import org.springframework.batch.test.MetaDataInstanceFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests {@link HeaderParsingLineCallbackHandler}
 */
class HeaderParsingLineCallbackHandlerTests {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    HeaderParsingLineCallbackHandler testee
    StepExecution stepExecution

    @Before
    void before() {
        stepExecution = MetaDataInstanceFactory.createStepExecution()
        testee = new HeaderParsingLineCallbackHandler(
                mappingSampleCodes: ['smpl1', 'smpl2'] as Set,
                defaultSuffix: 'var1',
                registeredSuffixes: ['var1', 'var2'] as Set,
                stepExecution: stepExecution,
                delegate: new HeaderSavingLineCallbackHandler(stepExecution: stepExecution)
        )
    }

    @Test
    void testEmptyHeader() {
        exception.expect(IllegalStateException)
        exception.expectMessage('No data columns are found.')

        testee.handleLine('ID_REF')
    }

    @Test
    void testSuccess() {
        testee.handleLine('ID_REF\tsmpl1.var1\tsmpl2.var1')

        assertThat stepExecution.executionContext.get(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY),
                allOf(instanceOf(Map),
                        hasEntry(equalTo('smpl1.var1'), allOf(
                                hasEntry('sample', 'smpl1'),
                                hasEntry('suffix', 'var1'),
                        ),
                        ),
                        hasEntry(equalTo('smpl2.var1'), allOf(
                                hasEntry('sample', 'smpl2'),
                                hasEntry('suffix', 'var1'),
                        )),
                )
    }

    @Test
    void testDefaultSuffix() {
        testee.defaultSuffix = 'var2'
        testee.handleLine('ID_REF\tsmpl1\tsmpl2')

        assertThat stepExecution.executionContext.get(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY),
                allOf(instanceOf(Map),
                        hasEntry(equalTo('smpl1'), allOf(
                                hasEntry('sample', 'smpl1'),
                                hasEntry('suffix', 'var2'),
                        )),
                        hasEntry(equalTo('smpl2'), allOf(
                                hasEntry('sample', 'smpl2'),
                                hasEntry('suffix', 'var2'),
                        )),
                )
    }

    @Test
    void testMoreThenOneSplitCandidate() {
        exception.expect(IllegalStateException)
        exception.expectMessage('Following are ambiguous column names: a.b')

        testee.mappingSampleCodes = ['a', 'a.b']
        testee.registeredSuffixes = ['def', 'b']
        testee.defaultSuffix = 'def'

        testee.handleLine('ID_REF\ta.b')
    }

    @Test
    void testAllSuffixesArePresentForAllSamples() {
        exception.expect(IllegalStateException)
        exception.expectMessage(allOf(
                startsWith('Following sample-suffix pairs have to be present in the header:'),
                containsString('smpl1.var2'),
                containsString('smpl2.var1'),
        ))

        testee.handleLine('ID_REF\tsmpl1.var1\tsmpl2.var2')
    }

    @Test
    void testDataContainsExtraSample() {
        exception.expect(IllegalStateException)
        exception.expectMessage(containsString("smpl3) do not match the sample codes"))

        testee.handleLine('ID_REF\tsmpl1\tsmpl2\tsmpl3')
    }

    @Test(expected = IllegalArgumentException)
    void testSkipUnmappedDataNotAllowedValue() {
        testee.skipUnmappedData = 'Yes'
    }

    @Test
    void testSkipUnmappedData() {
        testee.skipUnmappedData = 'Y'

        testee.handleLine('ID_REF\tsmpl1\tsmpl2\tsmpl3')

        assertThat stepExecution.executionContext.get(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY),
                allOf(instanceOf(Map),
                        hasEntry(equalTo('smpl1'), allOf(
                                hasEntry('sample', 'smpl1'),
                                hasEntry('suffix', 'var1'),
                        )),
                        hasEntry(equalTo('smpl2'), allOf(
                                hasEntry('sample', 'smpl2'),
                                hasEntry('suffix', 'var1'),
                        )),
                        hasEntry(equalTo('smpl3'), allOf(
                                hasEntry('sample', 'smpl3'),
                                hasEntry('suffix', 'var1'),
                        )),
                )
    }

    @Test
    void testDataFileMissesSamples() {
        exception.expect(IllegalStateException)
        exception.expectMessage('The data file misses samples that are declared in the mapping file: smpl2')

        testee.handleLine('ID_REF\tsmpl1')
    }

    @Test
    void testSampleSuffixCollision() {
        exception.expect(IllegalStateException)
        exception.expectMessage('Some columns represent the same information: [smpl1, smpl1.var1]')

        testee.defaultSuffix = 'var1'
        testee.registeredSuffixes = ['var1'] as Set
        testee.mappingSampleCodes = ['smpl1'] as Set
        testee.handleLine('ID_REF\tsmpl1\tsmpl1.var1')
    }

}
