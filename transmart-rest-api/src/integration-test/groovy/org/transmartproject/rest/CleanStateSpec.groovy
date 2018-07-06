package org.transmartproject.rest

import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
import org.transmartproject.rest.marshallers.MarshallerSpec
import spock.lang.Ignore

@Ignore // FIXME
class CleanStateSpec extends MarshallerSpec {

    void 'test that the database is empty'() {
        when: 'requesting all tree nodes'
        def response = getJson("${baseURL}/v2/tree_nodes")

        then:
        response.statusCode == HttpStatus.OK

        when: 'data is parsed'
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content) as Map
        def nodes = result['tree_nodes'] as List

        then: 'the result is empty'
        nodes.empty
    }

}
