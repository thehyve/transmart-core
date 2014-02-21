package org.transmartproject.db.marshallers

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.querytool.QtQueryResultInstance

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class QueryResultMarshallerTests {

    QueryResultMarshaller testee

    @Before
    void before() {
        testee = new QueryResultMarshaller()
    }

    @Test
    void basicTest() {
        def value = new QtQueryResultInstance(
                resultTypeId : 1,
                setSize      : 77,
                startDate    : new Date(),
                statusTypeId : 3,
                errorMessage : 'error message',
                description  : 'my description',
                deleteFlag   : 'Y'
        )
        value.id = -1L

        def out = testee.convert(value)
        assertThat out, allOf(
                hasEntry('errorMessage', 'error message'),
                hasEntry('id', -1L),
                hasEntry('setSize', 77L),
                hasEntry('status', QueryStatus.FINISHED),
                not(hasEntry(equalTo('statusTypeId'), anything())),
                not(hasEntry(equalTo('description'), anything())),
        )
    }
}
