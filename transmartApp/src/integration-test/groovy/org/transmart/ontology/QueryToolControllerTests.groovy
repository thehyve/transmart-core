package org.transmart.ontology

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.querytool.*
import org.transmartproject.core.users.User

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Integration
@Rollback
@WithGMock
class QueryToolControllerTests {

    QueryToolController testee

    @Before
    void before() {
        testee = new QueryToolController()
    }

    @Test
    void testRunQueryFromDefinition() {
        def queryDefinition = new QueryDefinition([])
        def testUsername = 'my_username'
        def resultInstance = new FakeQueryResult(username: testUsername)

        QueryDefinitionXmlConverter xmlService = mock(QueryDefinitionXmlConverter)
        xmlService.fromXml(anyOf(any(Reader), nullValue())).
                returns queryDefinition

        QueriesResource queriesService = mock(QueriesResource)
        queriesService.runQuery(is(queryDefinition), is(testUsername)).
                returns resultInstance

        User mockUser = mock(User)
        mockUser.username.returns testUsername

        testee.queryDefinitionXmlService = xmlService
        testee.queriesResourceAuthorizationDecorator = queriesService
        testee.currentUserBean = mockUser

        play {
            testee.runQueryFromDefinition()
        }

        def response = RequestContextHolder.requestAttributes.currentResponse
        assertThat response.contentType, startsWith('application/json')

        def json = new JsonSlurper().parseText(response.text)
        assertThat json, allOf(
                hasEntry('errorMessage', null),
                hasEntry('id', -1),
                hasEntry('setSize', 10),
                hasEntry('status', 'FINISHED'),
        )
    }
}

class FakeQueryResult implements QueryResult {
    Long id = -1
    Long setSize = 10
    QueryStatus status = QueryStatus.FINISHED
    String errorMessage = null
    String description = null
    Set patients = [] as Set
    String username
    QueryResultType queryResultType = null
    String name = null
    String queryXML = null
}
