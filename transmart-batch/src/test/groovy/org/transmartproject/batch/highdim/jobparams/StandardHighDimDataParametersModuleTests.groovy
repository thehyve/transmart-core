package org.transmartproject.batch.highdim.jobparams

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.InvalidParametersFileException

import java.nio.file.Path

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.startsWith

/**
 * Tests {@link StandardHighDimDataParametersModule}
 */
class StandardHighDimDataParametersModuleTests {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()
    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    StandardHighDimDataParametersModule testee = new StandardHighDimDataParametersModule()
    ExternalJobParametersInternalInterface params

    @Before
    void before() {
        def dataFileName = 'foo.txt'
        def dataFile = temporaryFolder.newFile(dataFileName)
        params = new ExternalJobParametersInternalInterface() {

            def map = [:]

            String getAt(String key) {
                map[key]
            }

            void putAt(String key, Object value) {
                map[key] = value
            }

            Path filePath = dataFile.toPath()

            String typeName = 'expression'
        }

        params['DATA_TYPE'] = 'R'
        params['LOG_BASE'] = '2'
        params['DATA_FILE'] = dataFileName
        params['ZERO_MEANS_NO_INFO'] = 'Y'
    }

    @Test
    void testValidateLogBaseDefault() {
        params['LOG_BASE'] = null

        testee.munge(params)

        assertThat params['LOG_BASE'], startsWith('2')
    }

    @Test
    void testValidateDataTypeDefault() {
        params['DATA_TYPE'] = null

        testee.munge(params)

        assertThat params['DATA_TYPE'], startsWith('R')
    }

    @Test
    void testValidateLogBaseMustBe2Based() {
        exception.expect(InvalidParametersFileException)
        exception.expectMessage(equalTo('LOG_BASE must be 2'))

        params['LOG_BASE'] = '10'

        testee.validate(params)
    }

    @Test
    void testDataTypeIsRequired() {
        params['DATA_TYPE'] = null

        exception.expect(InvalidParametersFileException)
        exception.expectMessage(equalTo('Parameter DATA_TYPE mandatory but not defined'))

        testee.validate(params)
    }

    @Test
    void testValidateSrcLogBaseSpecifiedWithWrongDataType() {
        params['DATA_TYPE'] = 'R'
        params['SRC_LOG_BASE'] = '10'

        exception.expect(InvalidParametersFileException)
        exception.expectMessage(equalTo('SRC_LOG_BASE could be specified only when DATA_TYPE=L'))

        testee.validate(params)
    }

    @Test
    void testValidateSrcLogBaseHasToBeSpecified() {
        params['DATA_TYPE'] = 'L'

        exception.expect(InvalidParametersFileException)
        exception.expectMessage(equalTo('SRC_LOG_BASE has to be specified when DATA_TYPE=L'))

        testee.validate(params)
    }
}
