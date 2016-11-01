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
 * Tests {@link HeaderSavingLineCallbackHandler}
 */
class HeaderSavingLineCallbackHandlerTests {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    HeaderSavingLineCallbackHandler testee
    StepExecution stepExecution

    @Before
    void before() {
        stepExecution = MetaDataInstanceFactory.createStepExecution()
        testee = new HeaderSavingLineCallbackHandler(
                stepExecution: stepExecution,
        )
    }

    @Test
    void testSuccess() {
        testee.handleLine('ID_REF\tsmpl1\tsmpl2')

        assertThat stepExecution.executionContext.get(HeaderSavingLineCallbackHandler.KEY),
                allOf(instanceOf(List), contains('ID_REF', 'smpl1', 'smpl2'))
    }

    @Test
    void testContradictingHeaderLines() {
        exception.expect(IllegalStateException)
        exception.expectMessage(allOf(
                containsString("content 'ID_REF\tsmpl2\tsmpl1'"),
                containsString("which I tokenized as: [ID_REF, smpl1, smpl2]"),
        ))

        testee.handleLine('ID_REF\tsmpl1\tsmpl2')
        //2nd line in the file. Could be also a header
        testee.handleLine('ID_REF\tsmpl2\tsmpl1')
    }

    @Test
    void testDuplicatedColumnName() {
        exception.expect(IllegalStateException)
        exception.expectMessage(allOf(
                startsWith('Following column names appear more then once: '),
                containsString('smpl1'),
                not(containsString('smpl2')),
                containsString('smpl3'),
        ))

        testee.handleLine('ID_REF\tsmpl1\tsmpl1\tsmpl2\tsmpl3\tsmpl3')
    }
}
