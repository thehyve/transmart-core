package org.transmartproject.batch.startup

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import java.nio.file.Path

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

/**
 * Tests {@link org.transmartproject.batch.startup.ExternalJobParametersModule}
 */
class ExternalJobParametersModuleTests implements ExternalJobParametersModule {

    public static final String BOOL_PARAM = 'BOOL_PARAM'
    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    ExternalJobParametersInternalInterface extJobParams

    @Before
    void before() {
        extJobParams = new ExternalJobParametersInternalInterface() {

            def map = [:]

            String getAt(String key) {
                map[key]
            }

            void putAt(String key, Object value) {
                map[key] = value
            }

            Path filePath = null

            String typeName = null
        }
    }

    @Test
    void testMungeBooleanToNegativeDefault() {
        mungeBoolean(extJobParams, BOOL_PARAM, false)

        assertThat extJobParams[BOOL_PARAM], equalTo('N')
    }

    @Test
    void testMungeBooleanToPositiveDefault() {
        mungeBoolean(extJobParams, BOOL_PARAM, true)

        assertThat extJobParams[BOOL_PARAM], equalTo('Y')
    }

    @Test
    void testMungeBooleanY() {
        extJobParams[BOOL_PARAM] = 'Y'

        mungeBoolean(extJobParams, BOOL_PARAM, false)

        assertThat extJobParams[BOOL_PARAM], equalTo('Y')
    }

    @Test
    void testMungeBooleanYes() {
        extJobParams[BOOL_PARAM] = 'Yes'

        mungeBoolean(extJobParams, BOOL_PARAM, false)

        assertThat extJobParams[BOOL_PARAM], equalTo('Y')
    }

    @Test
    void testMungeBooleanN() {
        extJobParams[BOOL_PARAM] = 'N'

        mungeBoolean(extJobParams, BOOL_PARAM, true)

        assertThat extJobParams[BOOL_PARAM], equalTo('N')
    }

    @Test
    void testMungeBooleanNo() {
        extJobParams[BOOL_PARAM] = 'No'

        mungeBoolean(extJobParams, BOOL_PARAM, true)

        assertThat extJobParams[BOOL_PARAM], equalTo('N')
    }

    @Test
    void testMungeBooleanUnsupportedArgument() {
        extJobParams[BOOL_PARAM] = 'x'

        exception.expect(InvalidParametersFileException)
        exception.expectMessage(
                equalTo("Unexpected argument ${extJobParams[BOOL_PARAM]} for boolean parameter ${BOOL_PARAM}."
                        .toString()))

        mungeBoolean(extJobParams, BOOL_PARAM, true)
    }

    @Override
    Set<String> getSupportedParameters() {
        [
                BOOL_PARAM,
        ] as Set
    }
}
