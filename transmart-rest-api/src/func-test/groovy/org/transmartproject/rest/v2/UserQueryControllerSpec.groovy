package org.transmartproject.rest

import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.TrueConstraint
import groovy.util.logging.Slf4j
import org.transmartproject.rest.v2.V2ResourceSpec

import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

@Slf4j
@Deprecated
class UserQueryControllerSpec extends V2ResourceSpec {

    def grailsApplication

    void 'test availability of query subscription'() {
        def body = createSampleQuery()
        def url = "${contextPath}/queries"
        log.info "Request URL: ${url}"

        when: "Notifications plugin is enabled"
        grailsApplication.config.org.transmartproject.notifications.enabled = true
        ResponseEntity<Resource> response = post(url, body)
        def result = toJson(response)

        then: "Query is saved correctly"
        response.statusCode.value() == 201
        result.subscribed == true
        result.subscriptionFreq == 'DAILY'

        when: "Notifications plugin is disabled"
        grailsApplication.config.org.transmartproject.notifications.enabled = false

        ResponseEntity<Resource> response2 = post(url, body)
        def result2 = toJson(response2)

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
