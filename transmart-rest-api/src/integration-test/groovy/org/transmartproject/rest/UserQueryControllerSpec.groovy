package org.transmartproject.rest

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.rest.marshallers.MarshallerSpec

@Slf4j
class UserQueryControllerSpec extends MarshallerSpec {

    def grailsApplication
    public static final String VERSION = "v2"

    void 'test availability of query subscription'() {
        def body = createSampleQuery()
        def url = "${baseURL}/$VERSION/queries"
        log.info "Request URL: ${url}"

        when: "Notifications plugin is enabled"
        grailsApplication.config.org.transmart.notifications.enabled = true
        ResponseEntity<Resource> response = postJson(url, body)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then: "Query is saved correctly"
        response.statusCode.value() == 201
        result.subscribed == true
        result.subscriptionFreq == 'DAILY'

        when: "Notifications plugin is disabled"
        grailsApplication.config.org.transmart.notifications.enabled = false

        ResponseEntity<Resource> response2 = postJson(url, body)
        String content2 = response2.body.inputStream.readLines().join('\n')
        def result2 = new JsonSlurper().parseText(content2)

        then: "It is not possible to subscribe for a query"
        response2.statusCode.value() == 503
        result2.message == "Subscription functionality is not enabled. Saving subscription data not supported."
    }

    private static Map createSampleQuery() {
        [
                name             : 'test query',
                patientsQuery    : [type: 'true'],
                observationsQuery: [type: Negation.name, arg: [type: TrueConstraint.name]],
                bookmarked       : true,
                subscribed       : true,
                subscriptionFreq : 'DAILY',
        ] as Map
    }
}
