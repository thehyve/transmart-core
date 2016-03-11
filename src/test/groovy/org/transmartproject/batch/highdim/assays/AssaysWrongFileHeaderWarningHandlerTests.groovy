package org.transmartproject.batch.highdim.assays

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Tests {@link AssaysWrongFileHeaderWarningHandler}
 */
class AssaysWrongFileHeaderWarningHandlerTests {

    AssaysWrongFileHeaderWarningHandler testee
    Logger rootLog
    ListAppender<ILoggingEvent> listAppender
    String delimiter = DELIMITER_TAB

    @Before
    void init() {
        testee = new AssaysWrongFileHeaderWarningHandler(new DelimitedLineTokenizer(delimiter: delimiter))
        rootLog = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        listAppender = new ListAppender<ILoggingEvent>()
        listAppender.start()
        rootLog.addAppender(listAppender)
    }

    @After
    void clear() {
        rootLog.detachAppender(listAppender)
    }

    @Test
    void testGiveNoWarnings() {
        testee.handleLine(['0', '1', '2', '3', '4', 'sample_type', 'tissue_type', 'time_point'].join(delimiter))

        assertThat listAppender.list, empty()
    }

    @Test
    void testGiveWarning() {
        testee.handleLine(['0', '1', '2', '3', '4', 'sample_type', 'tissue_type', 'attribute_2'].join(delimiter))

        assertThat listAppender.list, contains(
                hasProperty('level', equalTo(Level.WARN)),
        )
    }

    @Test
    void testGiveWarnings() {
        testee.handleLine(['0', '1', '2', '3', '4', 'tissue_type', 'attribute_1', 'attribute_2'].join(delimiter))

        assertThat listAppender.list, contains(
                hasProperty('level', equalTo(Level.WARN)),
                hasProperty('level', equalTo(Level.WARN)),
                hasProperty('level', equalTo(Level.WARN)),
        )
    }

}
