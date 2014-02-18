package org.transmartproject.db.ontology.http

import grails.test.mixin.TestFor
import org.junit.Test
import org.transmartproject.db.http.BusinessExceptionResolver

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(BusinessExceptionController)
class BusinessExceptionControllerTests {

    @Test
    void basicTest() {
        request.setAttribute(BusinessExceptionResolver
                .REQUEST_ATTRIBUTE_STATUS, 403)
        request.setAttribute(BusinessExceptionResolver
                .REQUEST_ATTRIBUTE_EXCEPTION, new RuntimeException('foo'))

        controller.index()

        assertThat response.status, is(equalTo(403))
        assertThat response.text, allOf(
                containsString('foo'),
                containsString('RuntimeException')
                )
    }
}
