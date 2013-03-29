package org.transmartproject.db.http

import grails.converters.JSON
import grails.test.mixin.*
import grails.test.mixin.support.*
import groovy.json.JsonSlurper
import org.junit.Before
import org.junit.Test
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.ontology.http.QueryToolController
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.querytool.QueriesResourceService
import org.transmartproject.db.querytool.QueryDefinitionXmlService

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class QueryToolControllerTests {

    QueryToolController testee

    @Before
    void before() {
        testee = new QueryToolController()
    }

    @Test
    void testRunQueryFromDefinition() {
        def queryDefinition = new QueryDefinition([])
        def resultInstance = new QueryResult() {
            Long id = -1
            Long setSize = 10
            QueryStatus status = QueryStatus.FINISHED
            String errorMessage = null
        };

        def xmlService = [
                fromXml: { queryDefinition }
        ] as QueryDefinitionXmlService
        def queriesService = [
                runQuery: {
                    assertThat it, sameInstance(queryDefinition)

                    resultInstance
                }
        ] as QueriesResourceService

        testee.queryDefinitionXmlService = xmlService
        testee.queriesResourceService = queriesService

        testee.runQueryFromDefinition()

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
